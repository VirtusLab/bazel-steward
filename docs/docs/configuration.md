---
layout: default
title: Configuration
nav_order: 3
---

You can configure how to handle specific dependencies. Config is stored in a root of a repository in `bazel-steward.yaml` file.

Example config:
```yaml
update-rules:
  -
    kinds: maven
    dependencies: commons-io:commons-io
    versioning: loose
    bumping: default
    pin: "2.0."
  -
    dependencies: io.get-coursier:interface
    versioning: semver
    bumping: latest
  -
    dependencies: org.jetbrains.kotlinx:kotlinx-coroutines-jdk8
    versioning: regex:^(?<major>\d*)(?:[.-](?<minor>(\d*)))?(?:[.-]?(?<patch>(\d*)))?(?:[-.]?(?<preRelease>(\d*)))(?<buildMetaData>)?
  -
    dependencies:
      - org.jetbrains.kotlinx:*
    enabled: false
  -
    versioning: loose
search-paths:
  -
    dependencies: "com.google:*"
    path-patterns:
      - "bazel/google_deps.bzl"
  -
    kinds: maven
    path-patterns:
      - "3rdparty/jvm/*.BUILD.bazel"
  -
    kinds: bazel
    path-patterns:
      - ".bazelversion"
      - ".github/**/*.yaml"
pull-requests:
  -   
    dependencies: "com.fasterxml.jackson*:*"
    group-id: jackson
  - 
    title: "[maintenance] Updated ${group}/${artifact} from ${versionFrom} to ${versionTo}"
    kinds: maven
  - 
    title: "[maintenance] Updated ${dependencyId}"
    limits:
      max-open: 5
      max-updates-per-run: 2
post-update-hooks:
  - kinds: maven
    commands:
      - "bazel run @unpinned_maven//:pin"
    files-to-commit:
      - "maven_install.json"
    run-for: commit
  - commands: "buildifier --lint=fix -r ."
    files-to-commit:
      - "**/*.bzl"
      - "**/BUILD.bazel"
      - "WORKSPACE"
    run-for: pull-request
    commit-message: "Apply buildifier"
```