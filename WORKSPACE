workspace(name = "bazel-steward")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# rules_jvm_external - for maven dependencies
RULES_JVM_EXTERNAL_TAG = "5.2"

RULES_JVM_EXTERNAL_SHA = "f86fd42a809e1871ca0aabe89db0d440451219c3ce46c58da240c7dcdc00125f"

http_archive(
    name = "rules_jvm_external",
    sha256 = RULES_JVM_EXTERNAL_SHA,
    strip_prefix = "rules_jvm_external-{}".format(RULES_JVM_EXTERNAL_TAG),
    url = "https://github.com/bazelbuild/rules_jvm_external/releases/download/5.2/rules_jvm_external-5.2.tar.gz",
)

IO_BAZEL_KOTLIN_RULES_TAG = "v1.9.5"

IO_BAZEL_KOTLIN_RULES_SHA = "34e8c0351764b71d78f76c8746e98063979ce08dcf1a91666f3f3bc2949a533d"

http_archive(
    name = "io_bazel_rules_kotlin",
    sha256 = IO_BAZEL_KOTLIN_RULES_SHA,
    url = "https://github.com/bazelbuild/rules_kotlin/releases/download/{}/rules_kotlin_release.tgz".format(IO_BAZEL_KOTLIN_RULES_TAG),
)

load("@io_bazel_rules_kotlin//kotlin:repositories.bzl", "kotlin_repositories")

kotlin_repositories()

register_toolchains("//:kotlin_toolchain")

# bazel_skylib - starlark functions
BAZEL_SKYLIB_TAG = "1.4.1"

BAZEL_SKYLIB_SHA = "b8a1527901774180afc798aeb28c4634bdccf19c4d98e7bdd1ce79d1fe9aaad7"

http_archive(
    name = "bazel_skylib",
    sha256 = BAZEL_SKYLIB_SHA,
    url = "https://github.com/bazelbuild/bazel-skylib/releases/download/{}/bazel-skylib-{}.tar.gz".format(BAZEL_SKYLIB_TAG, BAZEL_SKYLIB_TAG),
)

# io_bazel_rules_scala - required to avoid cyclic init error
IO_BAZEL_RULES_SCALA_TAG = "v6.1.0"

IO_BAZEL_RULES_SCALA_SHA = "cc590e644b2d5c6a87344af5e2c683017fdc85516d9d64b37f15d33badf2e84c"

http_archive(
    name = "io_bazel_rules_scala",
    sha256 = IO_BAZEL_RULES_SCALA_SHA,
    strip_prefix = "rules_scala-6.1.0",
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

# maven deps
load("@rules_jvm_external//:defs.bzl", "maven_install")

maven_install(
    artifacts = [
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0",
        "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.8.0",
        "io.get-coursier:interface:1.0.19",
        "commons-io:commons-io:2.16.1",
        "com.fasterxml.jackson.module:jackson-module-kotlin:2.17.0",
        "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.0",
        "com.fasterxml.jackson.core:jackson-core:2.17.0",
        "com.networknt:json-schema-validator:1.4.0",
        "org.jetbrains.kotlinx:kotlinx-cli-jvm:0.3.6",
        "org.kohsuke:github-api:1.321",
        "io.github.microutils:kotlin-logging-jvm:3.0.5",
        "org.slf4j:slf4j-simple:2.0.13",
        "org.json:json:20240303",
        "org.apache.commons:commons-text:1.12.0",
        "net.pearx.kasechange:kasechange-jvm:1.4.1",
        # tests
        "org.junit.platform:junit-platform-commons:1.10.2",
        "org.junit.platform:junit-platform-console:1.10.2",
        "org.junit.platform:junit-platform-engine:1.10.2",
        "org.junit.platform:junit-platform-launcher:1.10.2",
        "org.junit.platform:junit-platform-suite-api:1.10.2",
        "org.junit.jupiter:junit-jupiter-api:5.10.2",
        "org.junit.jupiter:junit-jupiter-engine:5.10.2",
        "org.junit.jupiter:junit-jupiter-params:5.10.2",
        "org.apiguardian:apiguardian-api:1.1.2",
        "org.opentest4j:opentest4j:1.3.0",
        "org.assertj:assertj-core:3.25.3",
        "io.kotest:kotest-assertions-api-jvm:5.8.1",
        "io.kotest:kotest-assertions-core-jvm:5.8.1",
        "io.kotest:kotest-assertions-shared-jvm:5.8.1",
        "io.kotest:kotest-common-jvm:5.8.1",
    ],
    fail_if_repin_required = True,
    fetch_sources = True,
    maven_install_json = "//:maven_install.json",
    repositories = [
        "https://repo.maven.apache.org/maven2",
    ],
)

load("@maven//:defs.bzl", "pinned_maven_install")

pinned_maven_install()
