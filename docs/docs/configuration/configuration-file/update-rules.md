---
layout: default
title: Update Rules
parent: Configuration File
grand_parent: Configuration
nav_order: 1
---

# Update Rules

This section is responsible for controlling how Bazel Steward selects a version to update.

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
```

Available fields:
  * `versioning` (string) <br/>
    Overrides what kind of versioning schema is used for the dependency.
    Default: `loose`. Allowed values: 
    - `loose` - always pick a newest version 
    - `semver` - update the dependency only if new version is backward compatible with currently used version according to [Semantic versioning](https://semver.org/)
    - `regex:...` - only update the dependency if it matches provided regex
  * `pin` (string) <br/>
    Filters versions that are allowed for the dependency.
    It can be an exact version, prefix or regular expression.
    Bazel Steward will try to automatically determine what kind of input it is.
    You can override this by prepending the value with `prefix:`, `exact:` or `regex:`.
  * `bumping` (string) <br/>
    Sets the strategy for bumping this dependency.
    1. `latest` - Always bump to the latest version.
    2. `patch` - First bump to the latest patch, then to the latest minor, and then finally to the latest major.
    3. `minor` - First bump to the latest minor, if there is no such update, bump the latest patch otheriwse bump the major.
    4. `latest-by-date` - Always bump to the most recently released version even if the version is lower then currently used.
  * `enabled` (boolean) <br/>
    If set to false, Bazel Steward will ignore this dependency for available versions lookup and any updates.
    If this is set for `kinds` only filter, then it will disable the specified kind - Bazel Steward will not attempt 
    to extract any versions used in your repository under this kind.