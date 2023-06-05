---
layout: default
title: GitHub Actions
parent: Installation
nav_order: 1
---

# GitHub Actions

This page will guide you through Github Actions configuration for Bazel Steward. It is the easies way to run it.

{: .note } 
Bazel Steward will run basic Bazel queries, so any non standard environment that is required for your bootstrapping your Bazel workspace needs to be ready before running Bazel Steward. 

Create a file at  `.github/workflows/` with this content:
```yaml
name: Bazel Steward

on:
  workflow_dispatch:
  schedule:
    - cron: '30 5 * * *' # runs every day at 5:30 am

  jobs:
    bazel-steward:
      runs-on: ubuntu-latest
      steps:
        - uses: actions/checkout@v2
          with:
            fetch-depth: 0
        - uses: VirtusLab/bazel-steward@latest
          with:
            github-personal-token: 'XXXXXXXXXXXXXXXXXX' # used for triggering workflows, read below
```

Make sure to allow Github Actions to create pull requests and give it write access so that Bazel Steward can push branches. You can find these settings
under `Settings / Actions / General / Workflow permissions`.

After every run, it creates a new branch and a pull request with a bumped version for every library it detects to be outdated.
You can merge the PR, close it, or push your changes into the branch.
Once a PR is opened, Bazel Steward will never open another one for the same version, regardless of what you do with the PR.

## Triggering workflows for pull requests

By design, GitHub Workflows don't trigger on pull requests when they are created by GitHub Actions.
To trigger workflows automatically, do one of the following:
* set `github-personal-token` to your personal token (recommended, your token will be used only for closing/reopening pull requests).
* set `github-token` to your personal token (every operation will be made in your name)
* create GitHub app and use its token for `github-token` (the best option, but requires a bit of setup)

Fine-grained personal access tokens require Read and Write access to code and pull requests.
Classic personal access tokens require `repo` and `write:discussion` permissions.
Note, that the organization owner may restrict the kind of token you can use.

Read more about triggering workflows using personal tokens and setting up the GitHub app [here](https://github.com/peter-evans/create-pull-request/blob/main/docs/concepts-guidelines.md#triggering-further-workflow-runs).

Read more about personal tokens [here](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token).

## Detailed usage
There are more variables that can be further used to customize how Bazel Steward is run:

```yaml
- uses: VirtusLab/bazel-steward@latest
  with:
    # The path to Bazel Steward configuration
    # Default: "./bazel-steward.yaml"
    configuration-path: ''
    
    # A token for the GitHub repository
    # Default: ${{ github.token }}
    github-token: ''
    
    # An optional token for closing and reopening pull requests
    github-personal-token: ''
    
    # Additional arguments to Bazel Steward
    # Example: "--base-branch dev --update-all-prs"
    additional-args: ''    
```