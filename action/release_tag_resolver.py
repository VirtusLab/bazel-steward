#!/usr/bin/env python3

from __future__ import annotations

import re
import subprocess
import sys
from dataclasses import dataclass
from functools import cmp_to_key
from typing import Iterable, Protocol

COMMIT_SHA_PATTERN = re.compile(r"^[0-9a-fA-F]{7,40}$")
FULL_COMMIT_SHA_PATTERN = re.compile(r"^[0-9a-fA-F]{40}$")
VERSION_SUFFIX = re.compile(r"(?:\.\d+)+$")


@dataclass(frozen=True)
class RepositoryTag:
    name: str
    commit_sha: str


class GhReleaseMetadataProvider(Protocol):
    def list_releases(self, repository: str) -> list[str]:
        ...

    def list_tags(self, repository: str) -> list[RepositoryTag]:
        ...

    def resolve_ref_to_commit_sha(self, repository: str, ref: str) -> str | None:
        ...


class ProcessGhReleaseMetadataProvider:
    def _run(self, *args: str) -> tuple[int, str]:
        completed = subprocess.run(
            args,
            check=False,
            capture_output=True,
            text=True,
        )
        return completed.returncode, f"{completed.stdout}{completed.stderr}"

    def list_releases(self, repository: str) -> list[str]:
        exit_code, output = self._run("gh", "release", "list", "-L", "100", "--repo", repository)
        if exit_code != 0:
            raise RuntimeError(f"gh release list failed for {repository} (exit {exit_code}): {output}")
        return [line for line in output.splitlines() if line.strip()]

    def list_tags(self, repository: str) -> list[RepositoryTag]:
        exit_code, output = self._run(
            "gh",
            "api",
            "--paginate",
            f"repos/{repository}/tags?per_page=100",
            "--jq",
            ".[] | [.name, .commit.sha] | @tsv",
        )
        if exit_code != 0:
            raise RuntimeError(f"gh api tags failed for {repository} (exit {exit_code}): {output}")
        tags: list[RepositoryTag] = []
        for line in output.splitlines():
            if not line.strip():
                continue
            parts = line.split("\t", maxsplit=1)
            if len(parts) != 2:
                continue
            tags.append(RepositoryTag(name=parts[0], commit_sha=parts[1]))
        return tags

    def resolve_ref_to_commit_sha(self, repository: str, ref: str) -> str | None:
        exit_code, output = self._run(
            "gh",
            "api",
            f"repos/{repository}/commits/{ref}",
            "--jq",
            ".sha",
        )
        if exit_code != 0:
            return None
        resolved = output.strip()
        if FULL_COMMIT_SHA_PATTERN.fullmatch(resolved):
            return resolved
        return None


class ReleaseTagResolver:
    @staticmethod
    def resolve(action_ref: str, repository: str, gh_metadata_provider: GhReleaseMetadataProvider) -> str:
        if action_ref.startswith("v"):
            resolved = ReleaseTagResolver.resolve_latest_matching_release(
                action_ref,
                gh_metadata_provider.list_releases(repository),
            )
            if resolved is None:
                raise RuntimeError(f"No GitHub release found matching tag pattern {action_ref} in {repository}")
            return resolved

        if ReleaseTagResolver.is_commit_sha(action_ref):
            commit_sha = action_ref
        else:
            commit_sha = gh_metadata_provider.resolve_ref_to_commit_sha(repository, action_ref)
            if commit_sha is None:
                raise RuntimeError(f"Could not resolve ref {action_ref} in {repository} to a commit SHA")

        resolved = ReleaseTagResolver.resolve_release_for_commit_sha(
            commit_sha_ref=commit_sha,
            repository=repository,
            gh_metadata_provider=gh_metadata_provider,
        )
        if resolved is None:
            raise RuntimeError(
                f"No GitHub release tag in {repository} points to commit {commit_sha} (resolved from {action_ref})",
            )
        return resolved

    @staticmethod
    def resolve_latest_matching_release(pattern: str, release_lines: list[str]) -> str | None:
        matching_tags: list[str] = []
        for line in release_lines:
            tag = ReleaseTagResolver.extract_release_tag(line)
            if tag is None:
                continue
            if ReleaseTagResolver.matches_tag_pattern(tag, pattern):
                matching_tags.append(tag)
        if not matching_tags:
            return None
        return max(matching_tags, key=cmp_to_key(ReleaseTagResolver.compare_version_tags))

    @staticmethod
    def matches_tag_pattern(tag: str, pattern: str) -> bool:
        if not tag.startswith(pattern):
            return False
        suffix = tag.removeprefix(pattern)
        return suffix == "" or bool(VERSION_SUFFIX.fullmatch(suffix))

    @staticmethod
    def compare_version_tags(left: str, right: str) -> int:
        left_parts = ReleaseTagResolver.version_parts(left)
        right_parts = ReleaseTagResolver.version_parts(right)
        max_size = max(len(left_parts), len(right_parts))
        for idx in range(max_size):
            left_value = left_parts[idx] if idx < len(left_parts) else 0
            right_value = right_parts[idx] if idx < len(right_parts) else 0
            if left_value != right_value:
                return 1 if left_value > right_value else -1
        return 0

    @staticmethod
    def version_parts(tag: str) -> list[int]:
        body = tag.removeprefix("v")
        if body == "":
            return [0]

        parts: list[int] = []
        for part in body.split("."):
            numeric_part = []
            for char in part:
                if char.isdigit():
                    numeric_part.append(char)
                else:
                    break
            parts.append(int("".join(numeric_part)) if numeric_part else 0)
        return parts

    @staticmethod
    def resolve_release_for_commit_sha(
        commit_sha_ref: str,
        repository: str,
        gh_metadata_provider: GhReleaseMetadataProvider,
    ) -> str | None:
        release_tags = {
            tag
            for tag in (
                ReleaseTagResolver.extract_release_tag(line)
                for line in gh_metadata_provider.list_releases(repository)
            )
            if tag is not None
        }
        if not release_tags:
            return None

        matching_tags: list[str] = []
        for repository_tag in gh_metadata_provider.list_tags(repository):
            if repository_tag.name not in release_tags:
                continue
            if ReleaseTagResolver.matches_commit_sha(repository_tag.commit_sha, commit_sha_ref):
                matching_tags.append(repository_tag.name)
        if not matching_tags:
            return None
        return max(matching_tags, key=cmp_to_key(ReleaseTagResolver.compare_version_tags))

    @staticmethod
    def extract_release_tag(line: str) -> str | None:
        parts = line.split("\t", maxsplit=1)
        if len(parts) != 2:
            return None
        return parts[0]

    @staticmethod
    def matches_commit_sha(commit_sha: str, commit_sha_ref: str) -> bool:
        normalized_commit_sha = commit_sha.lower()
        normalized_commit_sha_ref = commit_sha_ref.lower()
        return normalized_commit_sha == normalized_commit_sha_ref or normalized_commit_sha.startswith(
            normalized_commit_sha_ref,
        )

    @staticmethod
    def is_commit_sha(ref: str) -> bool:
        return bool(COMMIT_SHA_PATTERN.fullmatch(ref))


def resolve_tag(action_ref: str, repository: str) -> str:
    return ReleaseTagResolver.resolve(action_ref, repository, ProcessGhReleaseMetadataProvider())


def main(argv: list[str]) -> int:
    if len(argv) < 2:
        print("Usage: resolve-release-tag <action_ref> [repository]", file=sys.stderr)
        return 2
    action_ref = argv[1]
    repository = argv[2] if len(argv) > 2 else "VirtusLab/bazel-steward"
    try:
        print(resolve_tag(action_ref, repository))
        return 0
    except RuntimeError as exc:
        print(str(exc), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
