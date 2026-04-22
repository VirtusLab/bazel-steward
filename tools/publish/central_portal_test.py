"""Hermetic unit tests for the Central Portal client.

No network: `PortalClient.opener` is injected with a fake that records
requests and replays canned responses.
"""

from __future__ import annotations

import io
import json
import unittest
from typing import Any

from tools.publish.central_portal import (
    DEFAULT_BASE_URL,
    PortalClient,
    PortalError,
    StagingRepository,
    format_description,
    iter_profile_repos,
)


class _FakeResponse(io.BytesIO):
    def __enter__(self):
        return self

    def __exit__(self, *exc):
        self.close()
        return False


class _FakeOpener:
    """Minimal stand-in for `urllib.request.urlopen`.

    `script` is a list of `(method, path_suffix, response_json)` triples
    that are replayed in order. Each request pops the head of the list
    and asserts the method+path match.
    """

    def __init__(self, script: list[tuple[str, str, Any]]):
        self.script = list(script)
        self.calls: list[tuple[str, str, Any]] = []

    def __call__(self, req):
        method = req.get_method()
        url = req.full_url
        body = req.data and json.loads(req.data.decode("utf-8"))
        self.calls.append((method, url, body))
        assert self.script, f"No scripted response for {method} {url}"
        exp_method, exp_suffix, resp = self.script.pop(0)
        assert method == exp_method, f"expected {exp_method} got {method}"
        assert url.endswith(exp_suffix), f"url {url} did not end with {exp_suffix}"
        return _FakeResponse(json.dumps(resp).encode("utf-8"))


def _client(script) -> tuple[PortalClient, _FakeOpener]:
    opener = _FakeOpener(script)
    client = PortalClient(
        username="u",
        password="p",
        base_url=DEFAULT_BASE_URL,
        opener=opener,
    )
    return client, opener


class ListReposTest(unittest.TestCase):
    def test_filters_by_profile_and_normalizes_fields(self):
        script = [
            (
                "GET",
                "/staging/profile_repositories",
                {
                    "data": [
                        {
                            "repositoryId": "orgvirtuslab-1001",
                            "profileId": "abc",
                            "profileName": "org.virtuslab",
                            "type": "open",
                        },
                        {
                            "repositoryId": "other-9",
                            "profileId": "xyz",
                            "profileName": "com.example",
                            "type": "open",
                        },
                    ]
                },
            )
        ]
        client, _ = _client(script)
        repos = client.list_staging_repositories(profile_name="org.virtuslab")
        self.assertEqual(len(repos), 1)
        self.assertEqual(repos[0].repository_id, "orgvirtuslab-1001")
        self.assertEqual(repos[0].type, "open")


class FindOpenRepoTest(unittest.TestCase):
    def test_raises_when_none_open(self):
        script = [
            (
                "GET",
                "/staging/profile_repositories",
                {"data": []},
            )
        ]
        client, _ = _client(script)
        with self.assertRaises(PortalError):
            client.find_open_repository(profile_name="org.virtuslab")

    def test_raises_on_ambiguity(self):
        script = [
            (
                "GET",
                "/staging/profile_repositories",
                {
                    "data": [
                        {
                            "repositoryId": "a-1",
                            "profileId": "p",
                            "profileName": "org.virtuslab",
                            "type": "open",
                        },
                        {
                            "repositoryId": "a-2",
                            "profileId": "p",
                            "profileName": "org.virtuslab",
                            "type": "open",
                        },
                    ]
                },
            )
        ]
        client, _ = _client(script)
        with self.assertRaises(PortalError):
            client.find_open_repository(profile_name="org.virtuslab")

    def test_returns_single_open_repo(self):
        script = [
            (
                "GET",
                "/staging/profile_repositories",
                {
                    "data": [
                        {
                            "repositoryId": "a-1",
                            "profileId": "p",
                            "profileName": "org.virtuslab",
                            "type": "closed",
                        },
                        {
                            "repositoryId": "a-2",
                            "profileId": "p",
                            "profileName": "org.virtuslab",
                            "type": "open",
                        },
                    ]
                },
            )
        ]
        client, _ = _client(script)
        repo = client.find_open_repository(profile_name="org.virtuslab")
        self.assertEqual(repo.repository_id, "a-2")


class PureHelpersTest(unittest.TestCase):
    def test_format_description_is_stable(self):
        self.assertEqual(
            format_description("org.virtuslab", "bazel-steward", "1.7.2"),
            "Automated release of org.virtuslab:bazel-steward:1.7.2 via bazel-steward CI",
        )

    def test_iter_profile_repos_filters_correctly(self):
        repos = [
            StagingRepository("a", "p1", "org.virtuslab", "open"),
            StagingRepository("b", "p1", "org.virtuslab", "closed"),
            StagingRepository("c", "p2", "com.example", "open"),
        ]
        self.assertEqual(
            [r.repository_id for r in iter_profile_repos(repos, "org.virtuslab", "open")],
            ["a"],
        )


if __name__ == "__main__":
    unittest.main()
