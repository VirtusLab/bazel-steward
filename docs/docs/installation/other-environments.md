---
layout: default
title: Other environments
parent: Installation
nav_order: 2
---

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
coursier launch org.virtuslab:bazel-steward:0.2.18 --main org.virtuslab.bazelsteward.app.Main -- --help
```

## GitHub Releases
Bazel Steward publishes a fat JAR under GitHub Releases. The same JAR is also used in GitHub Actions. You can simply download it and run using the `java` command.

```
wget https://github.com/VirtusLab/bazel-steward/releases/download/v0.2.18/bazel-steward.jar
java -jar bazel-steward.jar --help
```