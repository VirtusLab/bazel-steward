workspace(name = "bazel-steward")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# rules_jvm_external - for maven dependencies
RULES_JVM_EXTERNAL_TAG = "4.4.2"

RULES_JVM_EXTERNAL_SHA = "735602f50813eb2ea93ca3f5e43b1959bd80b213b836a07a62a29d757670b77b"

http_archive(
    name = "rules_jvm_external",
    sha256 = RULES_JVM_EXTERNAL_SHA,
    strip_prefix = "rules_jvm_external-{}".format(RULES_JVM_EXTERNAL_TAG),
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/{}.zip".format(RULES_JVM_EXTERNAL_TAG),
)

IO_BAZEL_KOTLIN_RULES_TAG = "v1.6.0"

IO_BAZEL_KOTLIN_RULES_SHA = "a57591404423a52bd6b18ebba7979e8cd2243534736c5c94d35c89718ea38f94"

http_archive(
    name = "io_bazel_rules_kotlin",
    sha256 = IO_BAZEL_KOTLIN_RULES_SHA,
    url = "https://github.com/bazelbuild/rules_kotlin/releases/download/{}/rules_kotlin_release.tgz".format(IO_BAZEL_KOTLIN_RULES_TAG),
)

load("@io_bazel_rules_kotlin//kotlin:repositories.bzl", "kotlin_repositories")

kotlin_repositories()

register_toolchains("//:kotlin_toolchain")

# bazel_skylib - starlark functions
BAZEL_SKYLIB_TAG = "1.3.0"

BAZEL_SKYLIB_SHA = "74d544d96f4a5bb630d465ca8bbcfe231e3594e5aae57e1edbf17a6eb3ca2506"

http_archive(
    name = "bazel_skylib",
    sha256 = BAZEL_SKYLIB_SHA,
    url = "https://github.com/bazelbuild/bazel-skylib/releases/download/{}/bazel-skylib-{}.tar.gz".format(BAZEL_SKYLIB_TAG, BAZEL_SKYLIB_TAG),
)

# io_bazel_rules_scala - required to avoid cyclic init error
IO_BAZEL_RULES_SCALA_TAG = "20220201"

IO_BAZEL_RULES_SCALA_SHA = "77a3b9308a8780fff3f10cdbbe36d55164b85a48123033f5e970fdae262e8eb2"

http_archive(
    name = "io_bazel_rules_scala",
    sha256 = IO_BAZEL_RULES_SCALA_SHA,
    strip_prefix = "rules_scala-{}".format(IO_BAZEL_RULES_SCALA_TAG),
    url = "https://github.com/bazelbuild/rules_scala/releases/download/{}/rules_scala-{}.zip".format(IO_BAZEL_RULES_SCALA_TAG, IO_BAZEL_RULES_SCALA_TAG),
)

load("@io_bazel_rules_scala//:scala_config.bzl", "scala_config")

scala_config(scala_version = "2.13.6")

load("@io_bazel_rules_scala//scala:toolchains.bzl", "scala_register_toolchains")

scala_register_toolchains()

load("@io_bazel_rules_scala//scala:scala.bzl", "scala_repositories")

scala_repositories()

# junit5
load("//tools/build_rules:junit5.bzl", "junit_jupiter_java_repositories", "junit_platform_java_repositories")

JUNIT_JUPITER_VERSION = "5.8.2"

JUNIT_PLATFORM_VERSION = "1.8.2"

junit_jupiter_java_repositories(version = JUNIT_JUPITER_VERSION)

junit_platform_java_repositories(version = JUNIT_PLATFORM_VERSION)

# maven deps
load("@rules_jvm_external//:defs.bzl", "maven_install")

maven_install(
    artifacts = [
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4",
        "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.0",
        "io.get-coursier:interface:1.0.13",
        "commons-io:commons-io:2.11.0",
        "com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2",
        "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.14.2",
        "com.fasterxml.jackson.core:jackson-core:2.14.2",
        "com.networknt:json-schema-validator:1.0.78",
        "org.jetbrains.kotlinx:kotlinx-cli-jvm:0.3.5",
        "org.kohsuke:github-api:1.313",
        "io.github.microutils:kotlin-logging-jvm:3.0.4",
        "org.slf4j:slf4j-simple:2.0.7",
        "org.json:json:20220924",
    ],
    fetch_sources = True,
    repositories = [
        "https://repo.maven.apache.org/maven2",
    ],
)
