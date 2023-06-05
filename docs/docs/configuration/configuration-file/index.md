---
layout: default
title: Configuration File
parent: Configuration
has_children: true
nav_order: 1
---

# Configuration File
You can configure multiple aspects of Bazel Steward's behavior for example how to treat specific dependencies or how to create pull requests.

## Location
By default, Bazel Steward looks for the configuration file in the root of the git repository under one of the following names:
  * `.bazel-steward.yaml`
  * `bazel-steward.yaml`
  * `.bazel-steward.yml`
  * `bazel-steward.yml`

To customize the file, you can use the `--config-path` option. Pass a relative path to the configuration file.

## Example File
To quickly introduce yourself to the configuration format, take a look at the example below. Please read on below for detailed explanation of each field.

```yaml
update-rules:
  -
    kinds: maven
    dependencies: commons-io:commons-io
    versioning: loose
    bumping: patch
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
    branch-prefix: "update/"
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

## Rule Resolution
Bazel Steward has multiple configuration sections:
 * `update-rules`
 * `search-paths`
 * `pull-requests`
 * `post-update-hooks`

Each section contains a list of *rules*. Apart from fields specific to given section, each rule can contain special fileds used for filtering: `kinds` and `dependencies`.
* `kinds` - a kind of a dependency, can be one of `maven`, `bazel`, `bazel-rules`. Useful to for example specify specific search paths for all maven dependencies.
* `dependencies` - names of dependencies. Maven dependencies are in form of `"{group}:{artifact}"`. Wildcard in form of `*` is allowed. For advanced use cases it is possible to use regex for matching using `"regex:YOUR_REGEX"` syntax. Useful for library specific settings, like overriding versioning schema.

These fields can be either a single string value or a list of values. In case of a list, the search predicate uses `or`.

When resolving which rule to use, Bazel Steward first checks rules with the dependencies key defined (in order they are declared) and then other rules (also in declaration order).

## Sections
Check nested pages to read about each section.
