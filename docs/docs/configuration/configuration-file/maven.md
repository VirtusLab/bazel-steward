---
layout: default
title: Post Update Hooks
parent: Configuration File
grand_parent: Configuration
nav_order: 4
---

# Post Update Hooks

Bazel Steward is able to run arbitrary commands and commit their effects into its pull requests. It is useful for running tools like buildifier or pinning dependencies.

```yaml
post-update-hooks:
  - kinds: maven
    commands:
      - "REPIN=1 bazelisk run @unpinned_maven//:pin"
    files-to-commit:
      - "maven_install.json"
    run-for: commit
  - commands: "buildifier --lint=fix -r ."
    files-to-commit:
      - "**/*.bzl"
      - "**/BUILD.bazel"
      - "WORKSPACE.bazel"
    run-for: pull-request
    commit-message: "Apply buildifier"
```

Available fields:
  * `commands` (list of strings) <br/>
    List of commands to run after applying an update. Commands are run separately under `sh -c`
  * `files to commit` (list of strings) <br/>
    List of path patterns of files to commit after running the commands (syntax is the same as for `search-paths.path-patterns`).
  * `run-for` (string) <br/>
    Scope for running commands.
    1. `commit` - runs for each commit and includes changes in the commit
    2. `pull-request` - runs for the whole pull request, after creating all commits. It creates a separate commit with modified files. Message can be configured with the `commit-message` setting.
