"""Close and release the Sonatype staging repository after upload.

Expected to run *after* `bazel run //app:maven.publish` succeeds. The
upload created an OPEN staging repository on the Central Portal's
OSSRH-compatible endpoint; this tool transitions it through CLOSED and
RELEASED so the artifacts become visible on Maven Central without a
human clicking "Publish" in the UI.

Usage:
  bazel run //tools/publish:release_staged -- \\
      --profile org.virtuslab \\
      --group org.virtuslab \\
      --artifact bazel-steward \\
      --version 1.8.0

Env vars:
  SONATYPE_USERNAME  Central Portal user token name
  SONATYPE_PASSWORD  Central Portal user token secret
"""

from __future__ import annotations

import argparse
import logging
import os
import sys

from tools.publish.central_portal import (
    PortalClient,
    PortalError,
    format_description,
)

LOG = logging.getLogger("release_staged")


def _parse_args(argv: list[str]) -> argparse.Namespace:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--profile", required=True, help="Sonatype staging profile name")
    p.add_argument("--group", required=True, help="Maven groupId")
    p.add_argument("--artifact", required=True, help="Maven artifactId")
    p.add_argument("--version", required=True, help="Released version string")
    p.add_argument(
        "--dry-run",
        action="store_true",
        help="List the staging repo that would be released but don't close/release it.",
    )
    return p.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    logging.basicConfig(
        format="%(asctime)s %(levelname)s %(name)s: %(message)s",
        level=logging.INFO,
    )
    args = _parse_args(argv if argv is not None else sys.argv[1:])

    username = os.environ.get("SONATYPE_USERNAME")
    password = os.environ.get("SONATYPE_PASSWORD")
    if not username or not password:
        LOG.error("SONATYPE_USERNAME / SONATYPE_PASSWORD must be set.")
        return 2

    client = PortalClient(username=username, password=password)
    try:
        repo = client.find_open_repository(profile_name=args.profile)
        LOG.info(
            "Found open staging repo %s (profile=%s)",
            repo.repository_id,
            repo.profile_name,
        )
        if args.dry_run:
            LOG.info("--dry-run set; stopping before close/release.")
            return 0

        description = format_description(args.group, args.artifact, args.version)
        LOG.info("Closing %s: %s", repo.repository_id, description)
        client.close_repository(repo, description)
        LOG.info("Releasing %s: %s", repo.repository_id, description)
        client.release_repository(repo, description)
        LOG.info("Release complete for %s:%s:%s", args.group, args.artifact, args.version)
        return 0
    except PortalError as e:
        LOG.error("%s", e)
        return 1


if __name__ == "__main__":
    sys.exit(main())
