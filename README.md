# Bazel-Steward

Bazel-Steward is a bot that helps you keep your library dependencies up-to-date.

## How to work on the Project with IntelliJ

>Note: Be sure to install Bazelisk, Intellij with Kotlin and Scala Plugin.

### With Bazel Plugin installed

1. Ensure you have Bazel Plugin installed in your IntelliJ
2. Go to `Registry...`
3. Set `bazel.auto.import.disabled` to True.
4. If old project was created, you need to delete `.ijwb` directory
5. Build project again
6. Run setup-bsp.sh script
7. Open this project in IntelliJ

### Without Bazel Plugin installed

1. Build project
2. Run setup-bsp.sh script
3. Open this project in IntelliJ

