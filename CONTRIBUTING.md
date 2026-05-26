## Pre-commit hooks

CI runs [buildifier](https://github.com/bazelbuild/buildtools/tree/master/buildifier) and Bazel [ktlint](https://github.com/bazelbuild/rules_kotlin) checks. To catch and auto-fix those issues before push, install [pre-commit](https://pre-commit.com/):

```sh
pip install pre-commit   # or: brew install pre-commit
pre-commit install
```

On each commit, hooks format Starlark/Bazel files and Kotlin sources (via Bazel `*_lint_fix`, which runs `ktlint --format` with `//:lint_config`). If ktlint reformats a staged file, pre-commit fails until you re-stage and commit again. CI still runs the full `*_lint_test` suite.

To run everything manually (e.g. after a large edit):

```sh
pre-commit run --all-files
```

To fix all Kotlin lint violations in the repo (slower; same as `tools/lint_kotlin.sh`):

```sh
bazel test //... --test_tag_filters=lint --keep_going || true
tools/lint_kotlin.sh
```

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
3. Select all `./bazel-*` directories and mark them as excluded. Otherwise, they will pollute search scope and other features.

