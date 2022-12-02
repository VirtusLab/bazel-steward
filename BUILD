load("@io_bazel_rules_kotlin//kotlin:core.bzl", "define_kt_toolchain")

define_kt_toolchain(
    name = "kotlin_toolchain",
    api_version = "1.6",
    jvm_target = "11",
    language_version = "1.6",
)

load("@io_bazel_rules_kotlin//kotlin:lint.bzl", "ktlint_config")
load("@io_bazel_rules_kotlin//kotlin:lint.bzl", "ktlint_fix")
load("@io_bazel_rules_kotlin//kotlin:lint.bzl", "ktlint_test")

ktlint_config(
    name = "lint_config",
)

ktlint_test(
    name = "lint_test",
    srcs = glob(["**/*.kt"]),
)

ktlint_fix(
    name = "lint_fix",
    srcs = glob(["**/*.kt"]),
)
