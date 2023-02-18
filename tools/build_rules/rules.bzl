load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("//tools/build_rules:lint.bzl", "lint")
load("//tools/build_rules:junit5.bzl", "kt_junit5_test")

def default_target_name():
    _, _, target_name = native.package_name().rpartition("/")
    return target_name

def library(**kwargs):
    if "name" not in kwargs:
        kwargs["name"] = default_target_name()
    if "visibility" not in kwargs:
        kwargs["visibility"] = ["//visibility:public"]

    kt_jvm_library(**kwargs)
    lint(kwargs["srcs"], kwargs["name"])

def unit_tests(**kwargs):
    if "name" not in kwargs:
        kwargs["name"] = default_target_name()
    if "tags" not in kwargs:
        kwargs["tags"] = []
    kwargs["tags"].append("unit")

    kt_junit5_test(size = "small", **kwargs)
    lint(kwargs["srcs"], kwargs["name"])

def integration_tests(**kwargs):
    if "name" not in kwargs:
        kwargs["name"] = default_target_name()
    if "tags" not in kwargs:
        kwargs["tags"] = []
    kwargs["tags"].append("integration")

    kt_junit5_test(size = "large", **kwargs)
    lint(kwargs["srcs"], kwargs["name"])
