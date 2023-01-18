# Bazel-Steward

Bazel-Steward is a bot that helps you keep your library dependencies up-to-date.

## How to work on the Project with IntelliJ

**Note:** Be sure to install:
  * Bazelisk (instead of Bazel directly) 
  * IntelliJ with Kotlin and Scala Plugins

### If you have Bazel Plugin Installed

Bazel Plugin is useful for highlighting BUILD, WORKSPACE and .bzl files, but the project doesn't import well with it.
It will force import the project using its own model instead of Bazel-BSP. Follow the steps below to avoid this. 

1. Ensure you have Bazel Plugin installed in your IntelliJ
2. Go to `Registry...`
3. Set `bazel.auto.import.disabled` to True.
4. Close project in IntelliJ.
5. If old project was created, you need to delete `.ijwb` directory (most safe solution is to use `git clean -dfx`)

### Common Setup Steps

If you do not have Bazel Plugin or followed the instruction above, follow these steps:

1. Run setup-bsp.sh script
2. Open this project in IntelliJ
3. Select all `./bazel-*` directories and mark them as excluded. Otherwise they will pollute search scope and other features.

