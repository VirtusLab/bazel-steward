"""Bazel-native Maven Central publishing -- build-time artifact preparation.

Produces versioned artifacts (POM, JARs) and a JSON manifest that the
companion py_binary upload.py consumes at `bazel run` time.

Usage:
    bazel run --define MAVEN_VERSION=1.2.3 //tools/publish:maven_central
"""

def _maven_central_artifacts_impl(ctx):
    version = ctx.var.get("MAVEN_VERSION", "0.0.0")
    ws = ctx.workspace_name
    group_id = ctx.attr.group_id
    artifact_id = ctx.attr.artifact_id

    artifact_jar = ctx.file.artifact_jar
    source_jar = ctx.file.source_jar
    docs_jar = ctx.file.docs_jar
    pom = ctx.file.pom

    # -- 1. Version the POM via expand_template --
    versioned_pom = ctx.actions.declare_file(artifact_id + "-" + version + ".pom")
    ctx.actions.expand_template(
        template = pom,
        output = versioned_pom,
        substitutions = {
            "<version>0.0.0</version>": "<version>" + version + "</version>",
        },
    )

    # -- 2. Rename artifacts to Maven naming convention via symlink --
    named_jar = ctx.actions.declare_file(artifact_id + "-" + version + ".jar")
    ctx.actions.symlink(output = named_jar, target_file = artifact_jar)

    named_sources = ctx.actions.declare_file(artifact_id + "-" + version + "-sources.jar")
    ctx.actions.symlink(output = named_sources, target_file = source_jar)

    named_javadoc = ctx.actions.declare_file(artifact_id + "-" + version + "-javadoc.jar")
    ctx.actions.symlink(output = named_javadoc, target_file = docs_jar)

    # -- 3. Write JSON manifest for the upload script --
    maven_artifacts = [named_jar, named_sources, named_javadoc, versioned_pom]

    manifest = ctx.actions.declare_file(ctx.label.name + "_manifest.json")
    ctx.actions.write(
        output = manifest,
        content = json.encode({
            "workspace": ws,
            "group_id": group_id,
            "artifact_id": artifact_id,
            "version": version,
            "jar": named_jar.short_path,
            "sources": named_sources.short_path,
            "javadoc": named_javadoc.short_path,
            "pom": versioned_pom.short_path,
        }),
    )

    all_files = maven_artifacts + [manifest]
    runfiles = ctx.runfiles(files = all_files)

    return [DefaultInfo(
        files = depset([manifest]),
        runfiles = runfiles,
    )]

maven_central_artifacts = rule(
    implementation = _maven_central_artifacts_impl,
    attrs = {
        "artifact_jar": attr.label(allow_single_file = True, mandatory = True),
        "source_jar": attr.label(allow_single_file = True, mandatory = True),
        "docs_jar": attr.label(allow_single_file = True, mandatory = True),
        "pom": attr.label(allow_single_file = True, mandatory = True),
        "group_id": attr.string(mandatory = True),
        "artifact_id": attr.string(mandatory = True),
    },
)
