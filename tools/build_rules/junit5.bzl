# based on JetBrains/bazel-bsp integration

load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_test")

# External dependencies & java_junit5_test rule

def kt_junit5_test(name, srcs, test_package, deps = [], runtime_deps = [], **kwargs):
    FILTER_KWARGS = [
        "main_class",
        "use_testrunner",
        "args",
    ]

    for arg in FILTER_KWARGS:
        if arg in kwargs.keys():
            kwargs.pop(arg)

    junit_console_args = []
    if test_package:
        junit_console_args += ["--select-package", test_package]
    else:
        fail("must specify 'test_package'")

    return kt_jvm_test(
        name = name,
        srcs = srcs,
        main_class = "org.junit.platform.console.ConsoleLauncher",
        args = junit_console_args,
        deps = deps + [
            "@maven//:org_junit_jupiter_junit_jupiter_api",
            "@maven//:org_junit_jupiter_junit_jupiter_engine",
            "@maven//:org_junit_jupiter_junit_jupiter_params",
            "@maven//:org_junit_platform_junit_platform_suite_api",
            "@maven//:org_apiguardian_apiguardian_api",
            "@maven//:org_opentest4j_opentest4j",
            "@maven//:org_assertj_assertj_core",
            "@maven//:io_kotest_kotest_assertions_api_jvm",
            "@maven//:io_kotest_kotest_assertions_core_jvm",
            "@maven//:io_kotest_kotest_assertions_shared_jvm",
            "@maven//:io_kotest_kotest_common_jvm",
        ],
        runtime_deps = runtime_deps + [
            "@maven//:org_junit_platform_junit_platform_commons",
            "@maven//:org_junit_platform_junit_platform_console",
            "@maven//:org_junit_platform_junit_platform_engine",
            "@maven//:org_junit_platform_junit_platform_launcher",
        ],
        **kwargs
    )

def _format_maven_jar_name(group_id, artifact_id):
    return ("%s_%s" % (group_id, artifact_id)).replace(".", "_").replace("-", "_")

def _format_maven_jar_dep_name(group_id, artifact_id):
    return "@%s//jar" % _format_maven_jar_name(group_id, artifact_id)
