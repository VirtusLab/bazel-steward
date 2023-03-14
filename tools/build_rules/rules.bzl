load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("//tools/build_rules:lint.bzl", "lint")
load("//tools/build_rules:junit5.bzl", "kt_junit5_test")

def default_target_name():
    _, _, target_name = native.package_name().rpartition("/")
    return target_name

def ensure_name(kwargs):
    if "name" not in kwargs:
        kwargs["name"] = default_target_name()
    return kwargs["name"]

def ensure_size(kwargs, size):
    if "size" not in kwargs:
        kwargs["size"] = size

def ensure_visibility(kwargs):
    if "visibility" not in kwargs:
        kwargs["visibility"] = ["//visibility:public"]
    return kwargs["visibility"]

def ensure_tags(kwargs, *tags):
    if "tags" not in kwargs:
        kwargs["tags"] = tags
    else:
        current_tags = kwargs["tags"]
        for tag in tags:
            if tag not in current_tags:
                current_tags.append(tag)
    return kwargs["tags"]

def ensure_deps(kwargs, deps):
    if "deps" not in kwargs:
        kwargs["deps"] = deps
    else:
        current_deps = kwargs["deps"]
        for dep in deps:
            if dep not in current_deps:
                current_deps.append(dep)
    return kwargs["deps"]

def ensure_srcs(kwargs, srcs):
    if "srcs" not in kwargs:
        kwargs["srcs"] = srcs
    return kwargs["srcs"]

def library(**kwargs):
    ensure_name(kwargs)
    ensure_visibility(kwargs)
    ensure_srcs(kwargs, native.glob(["*.kt"]))

    kt_jvm_library(**kwargs)
    lint(kwargs["srcs"], kwargs["name"])

def resources(**kwargs):
    ensure_name(kwargs)
    ensure_srcs(kwargs, native.glob(["**/*"]))
    ensure_visibility(kwargs)
    native.filegroup(**kwargs)

def test_library(**kwargs):
    kwargs["testonly"] = True
    ensure_deps(kwargs, [
        "@io_kotest_kotest_common_jvm//jar",
        "@org_assertj_assertj_core//jar",
        "@org_junit_jupiter_junit_jupiter_api//jar",
    ])
    library(**kwargs)

def unit_tests(**kwargs):
    ensure_name(kwargs)
    ensure_tags(kwargs, "unit")
    kt_junit5_test(size = "small", **kwargs)
    lint(kwargs["srcs"], kwargs["name"])

def integration_tests(**kwargs):
    ensure_name(kwargs)
    ensure_tags(kwargs, "integration")
    ensure_size(kwargs, "medium")

    lint(kwargs["srcs"], kwargs["name"])
    return kt_junit5_test(**kwargs)

def integration_test_suite(**kwargs):
    name = ensure_name(kwargs)
    kwargs.pop("name")

    visibility = ensure_visibility(kwargs)

    tags = ensure_tags(kwargs, "integration")

    if "src" in kwargs:
        srcs = kwargs.pop("srcs")
    else:
        srcs = native.glob(["*Test.kt"])

    def target(src):
        target_name = "%s_%s" % (name, sanitize_name(src))
        integration_tests(
            name = target_name,
            srcs = [src],
            **kwargs
        )
        return target_name

    targets = [target(src) for src in srcs]
    return native.test_suite(name = name, tests = targets, visibility = visibility, tags = tags)

def sanitize_name(s):
    res = []
    for idx in range(len(s)):
        c = s[idx]
        if c.isalnum() or c == ".":
            res.append(c)
        else:
            res.append("_")
    return "".join(res)
