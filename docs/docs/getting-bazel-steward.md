---
layout: default
title: Getting Bazel Steward
nav_order: 1
---

### Maven Central
Bazel Steward is published to Maven Central under `org.virtuslab:bazel-steward`. The main class to run it is `org.virtuslab.bazelsteward.app.Main` (it is not present in the Manifest for now).

To easily run it, use [Coursier](https://get-coursier.io/docs/cli-installation).

```
coursier launch org.virtuslab:bazel-steward:0.2.18 --main org.virtuslab.bazelsteward.app.Main -- --help
```

### GitHub Releases
Bazel Steward publishes a fat JAR under GitHub Releases. The same JAR is also used in GitHub Actions. You can simply download it and run using the `java` command.

```
wget https://github.com/VirtusLab/bazel-steward/releases/download/v0.2.18/bazel-steward.jar
java -jar bazel-steward.jar --help
```