load("@rules_kotlin//kotlin:lint.bzl", "ktlint_fix", "ktlint_test")

def lint(srcs, name):
    ktlint_test(
        name = name + "_lint_test",
        srcs = srcs,
        config = "//:lint_config",
        tags = ["lint", "no-ide"],
    )

    ktlint_fix(
        name = name + "_lint_fix",
        srcs = srcs,
        config = "//:lint_config",
        tags = ["no-ide"],
    )
