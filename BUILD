load("@io_bazel_rules_kotlin//kotlin:core.bzl", "define_kt_toolchain")

define_kt_toolchain(
    name = "kotlin_toolchain",
    api_version = "1.6",
    jvm_target = "11",
    language_version = "1.6",
)

load("@io_bazel_rules_kotlin//kotlin:lint.bzl", "ktlint_config")

ktlint_config(
    name = "lint_config",
    editorconfig = ".editorconfig",
    visibility = ["//visibility:public"],
)
