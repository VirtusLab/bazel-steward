load("@io_bazel_rules_kotlin//kotlin:core.bzl", "define_kt_toolchain")
load("@io_bazel_rules_kotlin//kotlin:lint.bzl", "ktlint_config")

define_kt_toolchain(
    name = "kotlin_toolchain",
    api_version = "1.6",
    jvm_target = "11",
    language_version = "1.6",
)

ktlint_config(
    name = "lint_config",
    editorconfig = ".editorconfig",
    visibility = ["//visibility:public"],
)
