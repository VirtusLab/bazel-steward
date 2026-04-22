"""Client for Sonatype's Central Portal OSSRH-compatible staging API.

OSSRH (oss.sonatype.org) was sunset on 2025-06-30. Sonatype kept the
Nexus 2 REST surface alive at
`https://ossrh-staging-api.central.sonatype.com` specifically so existing
tooling (like `bazel_sonatype`, which speaks the Nexus 2 upload API) can
continue to work. After upload, deployments sit in a staging repository
that must be closed and then released before Maven Central picks them up.

This module wraps the three calls needed to finish a release after
`bazel run //app:maven.publish` has uploaded the artifacts:

  1. list open staging repositories for a profile
  2. close a staging repository (triggers validation)
  3. release a closed staging repository (promotes to Central)

References:
  https://central.sonatype.org/pages/ossrh-eol/
  https://help.sonatype.com/repomanager2/staging-releases/staging-rest-apis
"""

from __future__ import annotations

import base64
import dataclasses
import json
import logging
import time
import urllib.error
import urllib.request
from typing import Iterable, Optional

LOG = logging.getLogger(__name__)

DEFAULT_BASE_URL = "https://ossrh-staging-api.central.sonatype.com/service/local"

# Valid Nexus staging repository states we care about.
_STATE_OPEN = "open"
_STATE_CLOSED = "closed"
_STATE_RELEASED = "released"


class PortalError(RuntimeError):
    """Raised when the Central Portal staging API returns an error."""


@dataclasses.dataclass(frozen=True)
class StagingRepository:
    repository_id: str
    profile_id: str
    profile_name: str
    type: str  # "open" | "closed" | "released" | ...


@dataclasses.dataclass(frozen=True)
class PortalClient:
    """Minimal Nexus 2 staging API client.

    The client is intentionally tiny so it can be covered by a hermetic
    `py_test` without a network. All I/O goes through the injectable
    `opener` seam (defaults to `urllib.request.urlopen`).
    """

    username: str
    password: str
    base_url: str = DEFAULT_BASE_URL
    opener: callable = urllib.request.urlopen  # seam for tests

    # ---- public API ---------------------------------------------------

    def list_staging_repositories(
        self, profile_name: Optional[str] = None
    ) -> list[StagingRepository]:
        payload = self._request("GET", "/staging/profile_repositories")
        repos = [
            StagingRepository(
                repository_id=r["repositoryId"],
                profile_id=r["profileId"],
                profile_name=r["profileName"],
                type=r["type"],
            )
            for r in payload.get("data", [])
        ]
        if profile_name is not None:
            repos = [r for r in repos if r.profile_name == profile_name]
        return repos

    def find_open_repository(self, profile_name: str) -> StagingRepository:
        open_repos = [
            r
            for r in self.list_staging_repositories(profile_name=profile_name)
            if r.type == _STATE_OPEN
        ]
        if not open_repos:
            raise PortalError(
                f"No open staging repository found for profile {profile_name!r}. "
                "Did `bazel run //app:maven.publish` succeed?"
            )
        if len(open_repos) > 1:
            ids = ", ".join(r.repository_id for r in open_repos)
            raise PortalError(
                f"Multiple open staging repositories for profile "
                f"{profile_name!r}: {ids}. Refusing to pick one automatically."
            )
        return open_repos[0]

    def close_repository(self, repo: StagingRepository, description: str) -> None:
        self._bulk_action("close", repo, description)
        self._wait_for_state(repo, _STATE_CLOSED)

    def release_repository(self, repo: StagingRepository, description: str) -> None:
        self._bulk_action("promote", repo, description)
        self._wait_for_state(repo, _STATE_RELEASED, allow_missing=True)

    # ---- internals ----------------------------------------------------

    def _bulk_action(
        self, action: str, repo: StagingRepository, description: str
    ) -> None:
        body = {
            "data": {
                "stagedRepositoryIds": [repo.repository_id],
                "description": description,
                "autoDropAfterRelease": True,
            }
        }
        self._request("POST", f"/staging/bulk/{action}", body=body)

    def _wait_for_state(
        self,
        repo: StagingRepository,
        target_state: str,
        allow_missing: bool = False,
        timeout_s: int = 900,
        poll_interval_s: int = 15,
    ) -> None:
        deadline = time.monotonic() + timeout_s
        while True:
            try:
                current = self._get_repository(repo.repository_id)
            except PortalError as e:
                # "released" repos are dropped after release; treat 404 as success.
                if allow_missing and "404" in str(e):
                    LOG.info(
                        "Staging repo %s is gone (auto-dropped after release).",
                        repo.repository_id,
                    )
                    return
                raise
            LOG.info(
                "Staging repo %s state=%s (waiting for %s)",
                repo.repository_id,
                current.type,
                target_state,
            )
            if current.type == target_state:
                return
            if time.monotonic() > deadline:
                raise PortalError(
                    f"Timed out waiting for {repo.repository_id} to become "
                    f"{target_state!r}; last state was {current.type!r}."
                )
            time.sleep(poll_interval_s)

    def _get_repository(self, repository_id: str) -> StagingRepository:
        payload = self._request("GET", f"/staging/repository/{repository_id}")
        return StagingRepository(
            repository_id=payload["repositoryId"],
            profile_id=payload["profileId"],
            profile_name=payload["profileName"],
            type=payload["type"],
        )

    def _request(self, method: str, path: str, body: Optional[dict] = None) -> dict:
        url = f"{self.base_url.rstrip('/')}{path}"
        data = None
        headers = {
            "Accept": "application/json",
            "Authorization": "Basic "
            + base64.b64encode(
                f"{self.username}:{self.password}".encode("utf-8")
            ).decode("ascii"),
        }
        if body is not None:
            data = json.dumps(body).encode("utf-8")
            headers["Content-Type"] = "application/json"
        req = urllib.request.Request(url, data=data, method=method, headers=headers)
        try:
            with self.opener(req) as resp:
                raw = resp.read()
        except urllib.error.HTTPError as e:
            raise PortalError(
                f"{method} {url} returned {e.code}: {e.read().decode('utf-8', 'replace')}"
            ) from e
        if not raw:
            return {}
        return json.loads(raw)


def format_description(group: str, artifact: str, version: str) -> str:
    """Message attached to close/release actions.

    Factored out so it can be unit-tested and kept stable across runs.
    """
    return f"Automated release of {group}:{artifact}:{version} via bazel-steward CI"


def iter_profile_repos(
    repos: Iterable[StagingRepository], profile: str, state: str
) -> Iterable[StagingRepository]:
    """Pure helper; small enough to unit-test and keep the client simple."""
    for r in repos:
        if r.profile_name == profile and r.type == state:
            yield r
