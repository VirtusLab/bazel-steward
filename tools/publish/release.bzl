"""Starlark macros for the bazel-steward Maven release.

Keeps the release wiring in one place:
  * version comes from //tools/publish:version.bzl
  * coordinates are assembled from explicit (group, artifact) attrs
  * sonatype_java_export is wrapped so the call site in //app never has
    to know about stamping, GPG, or the Central Portal endpoint.

See tools/publish/release_staged.py for the companion Python tool that
closes and releases the staged Sonatype repository after `maven.publish`
finishes uploading.
"""

load("@bazel_sonatype//:defs.bzl", "sonatype_java_export")
load("//tools/publish:version.bzl", "RELEASE_VERSION")

def bazel_steward_release(
        name,
        group,
        artifact,
        maven_profile,
        pom_template,
        srcs,
        resources = None,
        runtime_deps = None,
        visibility = None):
    """Declares the publishable Maven artifact for bazel-steward.

    Expands to a `sonatype_java_export` with coordinates
    `<group>:<artifact>:<RELEASE_VERSION>` and the usual `<name>.publish`
    runner target consumed by the release pipeline.

    Args:
      name: Target name; the publish runner is `<name>.publish`.
      group: Maven groupId (e.g. `"org.virtuslab"`).
      artifact: Maven artifactId (e.g. `"bazel-steward"`).
      maven_profile: Sonatype namespace / staging profile.
      pom_template: Label of the pom.xml template.
      srcs: Sources for the jar.
      resources: Optional resources for the jar.
      runtime_deps: Optional runtime deps for the jar.
      visibility: Optional visibility for the generated targets.
    """
    sonatype_java_export(
        name = name,
        srcs = srcs,
        maven_coordinates = "{group}:{artifact}:{version}".format(
            group = group,
            artifact = artifact,
            version = RELEASE_VERSION,
        ),
        maven_profile = maven_profile,
        pom_template = pom_template,
        resources = resources or [],
        runtime_deps = runtime_deps or [],
        visibility = visibility,
    )
