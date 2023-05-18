---
layout: default
title: Rules
parent: Usage
nav_order: 2
---

When resolving which rule to use, Bazel Steward first checks rules with the dependencies key defined (in order they are declared) and then other rules (also in declaration order).

When the rule is found, it can configure for a dependency the following things:
* In `update-rules` section
  * `versioning` (string) <br/>
  Overrides what kind of versioning schema is used for the dependency.
  Default: `loose`. Allowed values: `loose`, `semver`, `regex:...`.
  * `pin` (string) <br/>
    Filters versions that are allowed for the dependency.
    It can be an exact version, prefix or regular expression.
    Bazel steward will try to automatically determine what kind of input it is.
    You can override this by prepending the value with `prefix:`, `exact:` or `regex:`.
  * `bumping` (string) <br/>
    Sets the strategy for bumping this dependency.
    1. `latest` - Bump to the latest version
    2. `default` - First bump to the latest patch, then to the latest minor, and then finally to the latest major.
    3. `minor` - First bump to the latest minor, and then to the latest major.
  * `enabled` (boolean) <br/>
    If set to false, Bazel Steward will ignore this dependency for available versions lookup and any updates.
    If this is set for `kinds` only filter, then it will disable the specified kind - Bazel Steward will not attempt 
    to extract any versions used in your repository under this kind.

* In `search-paths` section:
  * `path-patterns` (list of strings) <br/>
    Overrides paths where Bazel Steward will look for a version to update 
    (it is used by version replacement mechanism, not version detection). 
    A path pattern can either be:
    1. exact path: "exact:WORKSPACE.bazel"
    2. glob pattern: "glob:**/*.bzl"
    3. regex pattern: "regex:.test./BUILD(.bazel)?"
    
    All paths are relative to the Bazel workspace root. 
    The "exact:", "glob:", "regex:" prefixes are used to determine type of path pattern. 
    They can usually be omitted and correct syntax should be detected automatically.

* In `pull-requests` section:
  * `title` (string) <br/>
    Overrides template used for generating pull request title. Variables are inected with `${}` syntax.
    Available variables are `dependencyId`, `versionFrom`, `versionTo`, and for maven dependencies only: `group` and `artifact`.
  * `body` (string) <br/>
    Overrides template used for generating pull request body. Syntax and variables are the same as for the title.
  * `limits` (object) <br/>
      - `max-open` (number): maximum allowed number of open pull requests in the repository. Useful if you have 100 outdated dependencies and you would like to have just 10 open, merge them in your own pace, and Bazel Steward will add new pull requests up to this limit.
      - max-updates-per-run (number): maximum number of updated pull requests per Bazel Steward run. It includes both creating new PRs and resolving conflicts on existing ones. This is useful if your CI that runs on push is costly and you would like to limit the runs.
      
* In `post-update-hooks` section:
  * `commands` (list of strings) <br/>
    List of commands to run after applying an update. Commands are run separately under `sh -c`
  * `files to commit` (list of strings) <br/>
    List of path patterns of files to commit after running commands (syntax is the same as for `search-paths.path-patterns`).
  * `run-for` (string) <br/>
    Scope for running commands.
    1. `commit` - runs for each commit and includes changes in the commit
    2. `pull-request` - runs for the whole pull request, after creating all commits. It creates a separate commit with modifier file. Message can be configured with `commit-message` setting.
    Currently, each pull request has only one update since grouping is not implemented, so this setting only impact if new commit will be created or not.