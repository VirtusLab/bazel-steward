---
layout: default
title: Pull Requests
parent: Configuration File
grand_parent: Configuration
nav_order: 3
---

# Pull Requests
This section controls how Bazel Steward creates pull requests from the suggested updates (including grouping).

```yaml
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
```

Available fields:
  * `title` (string) <br/>
    Overrides template used for generating the pull request title. Variables are inected with `${}` syntax.
    Available variables are `dependencyId`, `versionFrom`, `versionTo`, and for maven dependencies only: `group` and `artifact`.
  * `body` (string) <br/>
    Overrides template used for generating the pull request body. Syntax and variables are the same as for the title.
  * `limits` (object) <br/>
      - `max-open` (number): maximum allowed number of open pull requests in the repository. Useful if you have 100 outdated dependencies and you would like to have just 10 open, merge them in your own pace, and Bazel Steward will add new pull requests up to this limit.
      - max-updates-per-run (number): maximum number of updated pull requests per Bazel Steward run. It includes both creating new PRs and resolving conflicts on existing ones. This is useful if your CI that runs on push is costly and you would like to limit the runs.
  * `group-id` (string) <br/>
    Enables grouping for dependencies matching filter (specified by `dependencies` and/or `kinds` keys).
    This id will be used in the branch name, the pull request title and the commit message.
    Note: Use this only for dependencies that are released together under the same version. Other scenarios might have unexpected results.
  * `branch-prefix` (string) <br/>
    Sets a prefix for branch name. Example: for prefix `update/` and Bazel updated to `7.0.0`, branch would be `update/bazel/7.0.0`. 
    Default prefix is `bazel-steward/`. Bazel Steward uses this prefix to count number of its open pull requests for the limits feature. 
    If you intend to use this feature, use a unique prefix so that your other PRs don't count against this limit.
