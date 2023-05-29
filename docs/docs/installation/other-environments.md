---
layout: default
title: Other environments
parent: Installation
nav_order: 2
---

## Environment Requirements
* Bazel commands complete succesfully in the environment
  * It is best to run Bazel Steward in the same environment that is used for Bazel builds on your CI. Minimally, your workspace needs to initialize correctly to allow running queries. 
  * If your repository uses pinned maven dependecies, you will want to run `bazel run @unpinned_maven//:pin` as an `post-update-hook`. This command also needs to be able to run correctly in your environment.
* Git write access - for pushing branches (Bazel Steward only pushes its own branches, it doesn't touch master branch for example)
* Configure Git author in the environment (name and email) that will be used for creating branches
* Configure `GITHUB_TOKEN` environment variable for fetching available rules versions (it can work without the token, but expect exceeding API limits)
* Set `--github` flag - Bazel Steward now only works with GitHub as a platform. Without the flag, it will only push the branches, but is not able to check PR status or open/close them.
  * Set `GITHUB_TOKEN` env - necessary for PR management
  * Set `GITHUB_REPOSITORY` env to your repository location. Example value: `VirtusLab/bazel-steward` 
  * Optionally set `GITHUB_API_URL` if you are not using the public GitHub. Default is `https://api.github.com`
