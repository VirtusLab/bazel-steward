load("@rules_kotlin//kotlin:core.bzl", "define_kt_toolchain")
load("@rules_kotlin//kotlin:lint.bzl", "ktlint_config")

define_kt_toolchain(
    name = "kotlin_toolchain",
    api_version = "1.7",
    jvm_target = "11",
    language_version = "1.7",
)

ktlint_config(
    name = "lint_config",
    editorconfig = ".editorconfig",
    visibility = ["//visibility:public"],
)
