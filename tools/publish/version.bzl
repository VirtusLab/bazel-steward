"""Release version for the publishable `bazel-steward` Maven artifact.

This file exists to make the version injection point explicit. It is the
single source of truth consumed by `//tools/publish:release.bzl`.

Local/dev builds always see `0.0.0`. During a release, CI rewrites this
file via `tools/publish/set_version.py` before running
`bazel run //app:maven.publish`, and reverts it after.

Why not a stamp key in `maven_coordinates`? `bazel_sonatype` 1.1.1 (and
its underlying `rules_jvm_external.pom_file`) substitute coordinates at
analysis time via `ctx.actions.expand_template` with static strings —
they do not read workspace status keys. Proper stamping would require a
`bazel_sonatype` fork or a local sonatype rule; tracked as follow-up.
"""

RELEASE_VERSION = "0.0.0"
