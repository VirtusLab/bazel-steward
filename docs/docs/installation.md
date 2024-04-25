---
layout: default
title: Installation
nav_order: 2
---

# Installation

You can use Bazel Steward:
  * as a GitHub Action
  * directly, either from Maven Central or using a JAR from Github Releases - if you need to use other CI environment

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

# Other Environments
Bazel Steward is just a JVM application. If it has access to Git, Bazel and expected tokens, you can simply run it as any other Java application and it will setup all
pull requests according to the configuration.

First, check if environment where you plan to run Bazel Steward (for example a Jenkins server) meets all the requirements.

## Environment Requirements
* Bazel commands complete succesfully in the environment
  * It is best to run Bazel Steward in the same environment that is used for Bazel builds on your CI. Bazel needs to at least initialize correctly in your workspace to allow running queries. 
  * If your repository uses pinned maven dependecies, you will want to run `bazel run @unpinned_maven//:pin` as an `post-update-hook`. This command also needs to be able to run correctly in your environment.
* Git write access - for pushing branches (Bazel Steward only pushes its own branches, it doesn't touch `main` branch or your pull requests)
* Configure Git author in the environment (name and email) that will be used for creating branches
* Configure `GITHUB_TOKEN` environment variable for fetching available rules versions (it can work without the token, but expect exceeding API limits)
* Set `--github` flag - Bazel Steward now only works with GitHub as a platform. Without the flag, it will only push the branches, but is not able to check PR status or open/close them.
  * Set `GITHUB_TOKEN` env - necessary for PR management
  * Set `GITHUB_REPOSITORY` env to your repository location. Example value: `VirtusLab/bazel-steward` 
  * Optionally set `GITHUB_API_URL` if you are not using the public GitHub. Default is `https://api.github.com`

Bazel Steward is available in Maven Central and GitHub Releases.

## Maven Central
Bazel Steward is published to Maven Central under `org.virtuslab:bazel-steward`. The main class to run it is `org.virtuslab.bazelsteward.app.Main` (it is not present in the Manifest for now).

To easily run it, use [Coursier](https://get-coursier.io/docs/cli-installation).

```
coursier launch org.virtuslab:bazel-steward:1.5.0 --main org.virtuslab.bazelsteward.app.Main -- --help
```

## GitHub Releases
Bazel Steward publishes a fat JAR under GitHub Releases. The same JAR is also used in GitHub Actions. You can simply download it and run using the `java` command.

```
wget https://github.com/VirtusLab/bazel-steward/releases/download/v1.5.0/bazel-steward.jar
java -jar bazel-steward.jar --help
```
