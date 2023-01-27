load("@io_bazel_rules_kotlin//kotlin:lint.bzl", "ktlint_fix", "ktlint_test")

def lint(srcs, name):
    ktlint_test(
        name = "lint_test" + name,
        srcs = srcs,
        config = "//:lint_config",
        tags = ["lint", "no-ide"],
    )

    ktlint_fix(
        name = "lint_fix" + name,
        srcs = srcs,
        config = "//:lint_config",
        tags = ["no-ide"],
    )
