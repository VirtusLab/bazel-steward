"""Rewrite //tools/publish:version.bzl for a release.

Used by CI between `actions/checkout` and `bazel run //app:maven.publish`
so that the coordinates baked into the pom.xml pick up the release
version instead of the default `0.0.0`.

Kept as a standalone script (invoked via `python3 tools/publish/set_version.py`)
rather than a `py_binary` so CI can call it before any Bazel action runs,
without requiring a Bazel server start just for a one-line file rewrite.

Usage:
  python3 tools/publish/set_version.py --version 1.8.0
  python3 tools/publish/set_version.py --restore
"""

from __future__ import annotations

import argparse
import pathlib
import re
import sys

VERSION_BZL = pathlib.Path(__file__).resolve().parent / "version.bzl"
DEFAULT_VERSION = "0.0.0"
VERSION_PATTERN = re.compile(r'^(RELEASE_VERSION\s*=\s*")([^"]+)(")', re.MULTILINE)


def _rewrite(new_version: str) -> None:
    text = VERSION_BZL.read_text()
    new_text, count = VERSION_PATTERN.subn(
        lambda m: f"{m.group(1)}{new_version}{m.group(3)}", text, count=1
    )
    if count != 1:
        raise SystemExit(
            f"Could not find RELEASE_VERSION assignment in {VERSION_BZL}"
        )
    VERSION_BZL.write_text(new_text)


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(description=__doc__)
    group = p.add_mutually_exclusive_group(required=True)
    group.add_argument("--version", help="Version string to inject.")
    group.add_argument(
        "--restore",
        action="store_true",
        help=f"Restore RELEASE_VERSION to the default ({DEFAULT_VERSION}).",
    )
    args = p.parse_args(argv if argv is not None else sys.argv[1:])

    target = DEFAULT_VERSION if args.restore else args.version
    if not re.fullmatch(r"[0-9A-Za-z.\-+]+", target):
        p.error(f"Refusing to inject suspicious version string: {target!r}")
    _rewrite(target)
    print(f"{VERSION_BZL.name}: RELEASE_VERSION = \"{target}\"")
    return 0


if __name__ == "__main__":
    sys.exit(main())
