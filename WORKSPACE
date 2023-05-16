workspace(name = "bazel-steward")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# rules_jvm_external - for maven dependencies
RULES_JVM_EXTERNAL_TAG = "4.5"

RULES_JVM_EXTERNAL_SHA = "b17d7388feb9bfa7f2fa09031b32707df529f26c91ab9e5d909eb1676badd9a6"

http_archive(
    name = "rules_jvm_external",
    sha256 = RULES_JVM_EXTERNAL_SHA,
    strip_prefix = "rules_jvm_external-{}".format(RULES_JVM_EXTERNAL_TAG),
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/4.5.zip",
)

IO_BAZEL_KOTLIN_RULES_TAG = "v1.7.1"

IO_BAZEL_KOTLIN_RULES_SHA = "fd92a98bd8a8f0e1cdcb490b93f5acef1f1727ed992571232d33de42395ca9b3"

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
IO_BAZEL_RULES_SCALA_TAG = "v5.0.0"

IO_BAZEL_RULES_SCALA_SHA = "141a3919b37c80a846796f792dcf6ea7cd6e7b7ca4297603ca961cd22750c951"

http_archive(
    name = "io_bazel_rules_scala",
    sha256 = IO_BAZEL_RULES_SCALA_SHA,
    strip_prefix = "rules_scala-5.0.0",
    url = "https://github.com/bazelbuild/rules_scala/archive/refs/tags/{}.tar.gz".format(IO_BAZEL_RULES_SCALA_TAG),
)

load("@io_bazel_rules_scala//:scala_config.bzl", "scala_config")

scala_config(scala_version = "2.13.6")

load("@io_bazel_rules_scala//scala:toolchains.bzl", "scala_register_toolchains")

scala_register_toolchains()

load("@io_bazel_rules_scala//scala:scala.bzl", "scala_repositories")

scala_repositories()

# rules_proto for sonatype

RULES_PROTO_TAG = "4.0.0-3.20.0"

RULES_PROTO_SHA = "e017528fd1c91c5a33f15493e3a398181a9e821a804eb7ff5acdd1d2d6c2b18d"

http_archive(
    name = "rules_proto",
    sha256 = RULES_PROTO_SHA,
    strip_prefix = "rules_proto-{}".format(RULES_PROTO_TAG),
    urls = [
        "https://github.com/bazelbuild/rules_proto/archive/refs/tags/{}.tar.gz".format(RULES_PROTO_TAG),
    ],
)

load("@rules_proto//proto:repositories.bzl", "rules_proto_dependencies", "rules_proto_toolchains")

rules_proto_dependencies()

rules_proto_toolchains()

# sonatype for publishing
BAZEL_SONATYPE_TAG = "1.1.1"

BAZEL_SONATYPE_SHA = "6d1bc7da15dae958274df944eb46e9757e14187cda6decd66fc1aeeb1ea21758"

http_archive(
    name = "bazel_sonatype",
    sha256 = BAZEL_SONATYPE_SHA,
    strip_prefix = "bazel-sonatype-{}".format(BAZEL_SONATYPE_TAG),
    url = "https://github.com/JetBrains/bazel-sonatype/archive/v{}.zip".format(BAZEL_SONATYPE_TAG),
)

load("@bazel_sonatype//:defs.bzl", "sonatype_dependencies")

sonatype_dependencies()

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
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1",
        "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.0",
        "io.get-coursier:interface:1.0.16",
        "commons-io:commons-io:2.11.0",
        "com.fasterxml.jackson.module:jackson-module-kotlin:2.15.0",
        "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0",
        "com.fasterxml.jackson.core:jackson-core:2.15.0",
        "com.networknt:json-schema-validator:1.0.81",
        "org.jetbrains.kotlinx:kotlinx-cli-jvm:0.3.5",
        "org.kohsuke:github-api:1.314",
        "io.github.microutils:kotlin-logging-jvm:3.0.4",
        "org.slf4j:slf4j-simple:2.0.7",
        "org.json:json:20230227",
        "org.apache.commons:commons-text:1.10.0",
        "net.pearx.kasechange:kasechange-jvm:1.4.1",
    ],
    fail_if_repin_required = True,
    fetch_sources = True,
    fail_if_repin_required = True,
    repositories = [
        "https://repo.maven.apache.org/maven2",
    ],
    maven_install_json = "//:maven_install.json",
)

load("@maven//:defs.bzl", "pinned_maven_install")
pinned_maven_install()