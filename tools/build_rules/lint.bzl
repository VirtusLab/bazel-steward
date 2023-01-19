load("@io_bazel_rules_kotlin//kotlin:lint.bzl", "ktlint_fix", "ktlint_test")

def lint(srcs):
    ktlint_test(
        name = "lint_test",
        srcs = srcs,
        config = "//:lint_config",
        tags = ["lint", "no-ide"],
    )

    ktlint_fix(
        name = "lint_fix",
        srcs = srcs,
        config = "//:lint_config",
        tags = ["no-ide"],
    )
