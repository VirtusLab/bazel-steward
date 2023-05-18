---
layout: default
title: Command line arguments
parent: Usage
nav_order: 1
---

```
Arguments:
    repository [.] -> Location of the local repository to scan (optional)
Options:
    --github [false] -> Use GitHub as platform
    --no-remote, -n [false] -> Do not push to remote
    --update-all-prs, -f [false] -> Update all pull requests (force push even if PR is open and has no conflicts)
    --base-branch -> Branch that will be set as a base in pull request (default: current branch)
    --config-path -> Path to the config file (default: `.bazel-steward.yaml`)
```