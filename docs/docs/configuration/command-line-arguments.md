---
layout: default
title: Command Line Arguments
parent: Configuration
nav_order: 2
---

# Command Line Arguments
Bazel Steward is a command line application. You can use the arguments below if you are setting up Bazel Steward manually, or through the `additional-args` parameter in GitHub Actions.

```
Arguments:
    repository [.] -> Location of the local repository to scan (optional)
Options:
    --github -> Use GitHub as a platform (default: no platform)
    --no-remote, -n -> Do not push to remote (default: pushes to remote)
    --update-all-prs, -f -> Update all pull requests (force push even if PR is open and has no conflicts)
    --base-branch -> Branch that will be set as a base in each pull request (default: current branch)
    --config-path -> Path to the config file (default: `bazel-steward.yaml`)
    --no-internal-config -> Do not load the [internal default config](https://github.com/VirtusLab/bazel-steward/blob/main/app/src/main/resources/internal-config.yaml)
    --analyze-only, -a -> Only analyze what updates are needed (useful if you want to manually update selected dependencies)
    --allow-dirty-workspace -> Allow running with uncommitted/untracked local changes by discarding them
```

## Details

When `--allow-dirty-workspace` is enabled, Bazel Steward will not fail on a dirty working tree.  
During branch switching it may use forced checkout and discard local uncommitted changes (including untracked files) in the repository checkout.
If you need to keep local generated artifacts (for example, external GitHub Action cache files), add their paths to `.gitignore` so Bazel Steward ignores them.