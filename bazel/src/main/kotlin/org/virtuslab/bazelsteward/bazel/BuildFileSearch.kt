package org.virtuslab.bazelsteward.bazel

import org.virtuslab.bazelsteward.core.Workspace
import java.nio.file.Files
import java.nio.file.Path

class BuildFileSearch(workspace: Workspace) {

    private val fileNamesRegex =
        Regex(listOf("""BUILD\.bazel""", "BUILD", "WORKSPACE", """[0-9a-zA-Z]+\.bzl""").reduce { acc, s -> "$acc|$s" })

    private val buildPaths: List<Path> by lazy {
        workspace.path.toFile().walkBottomUp()
            .filter { it.isFile && fileNamesRegex.matches(it.name) }
            .map { it.toPath() }
            .toList()
    }

    val buildDefinitions: List<Pair<Path, String>> by lazy {
        buildPaths.map { it to String(Files.readAllBytes(it)) }
    }

}