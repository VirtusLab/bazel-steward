---
layout: default
title: Search Paths
parent: Configuration File
grand_parent: Configuration
nav_order: 2
---

# Search Paths

Bazel Steward uses Bazel directly to extract what versions is your repository using. It doesn't parse the configuration directly. However it needs to use heuristics to find and update a place where the dependency was configured. This section is responsible for controlling where Bazel Steward looks for versions to replace.

```yaml
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
```

Available fields:
  * `path-patterns` (list of strings) <br/>
    Overrides paths where Bazel Steward will look for a version to update 
    (it is used by version replacement mechanism, not version detection). 
    A path pattern can either be:
    1. exact path: `"exact:WORKSPACE.bazel"`
    2. glob pattern: `"glob:**/*.bzl"`
    3. regex pattern: `"regex:.*test.*/BUILD(.bazel)?"`
    
    All paths are relative to the Bazel workspace root. 
    The `"exact:"`, `"glob:"`, `"regex:"` prefixes are used to determine type of path pattern. 
    They can usually be omitted and correct syntax should be detected automatically.
