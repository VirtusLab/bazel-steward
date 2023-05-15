package org.virtuslab.bazelsteward.e2e

import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.virtuslab.bazelsteward.app.PullRequestSuggestion
import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.NewPullRequest
import org.virtuslab.bazelsteward.core.common.CommitRequest
import org.virtuslab.bazelsteward.core.common.FileChange
import org.virtuslab.bazelsteward.e2e.fixture.E2EBase
import org.virtuslab.bazelsteward.maven.MavenCoordinates
import java.nio.file.Path

class GroupingTest : E2EBase() {
  @Test
  fun `should group common maven dependencies according to the config`(@TempDir tempDir: Path) {
    val project = "maven/group"
    val workspace = prepareWorkspace(tempDir, project)
    val result = runBazelStewardWith(workspace) {
      it.withMockMaven {
        withVersion("com.carrotsearch:hppc", "0.9.5")
        withVersion("org.jline:jline", "3.21.0")
        withVersion(
          listOf(
            "com.fasterxml.jackson.core:jackson-annotations",
            "com.fasterxml.jackson.core:jackson-core",
            "com.fasterxml.jackson.core:jackson-databind",
            "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml",
            "com.fasterxml.jackson.datatype:jackson-datatype-guava",
            "com.fasterxml.jackson.datatype:jackson-datatype-jdk8",
            "com.fasterxml.jackson.datatype:jackson-datatype-jsr310",
          ),
          "2.15.0",
        )
        withVersion(
          listOf(
            "org.glassfish.jersey.containers:jersey-container-grizzly2-http",
            "org.glassfish.jersey.core:jersey-client",
            "org.glassfish.jersey.core:jersey-common",
            "org.glassfish.jersey.core:jersey-server",
            "org.glassfish.jersey.inject:jersey-hk2",
            "org.glassfish.jersey.media:jersey-media-json-jackson",
          ),
          "2.40.0",
        )
      }
    }

    val prSuggestions = result.keys

    val expectedSuggestions = setOf(
      PullRequestSuggestion(
        NewPullRequest(
          GitBranch("bazel-steward/com.carrotsearch/hppc/0.9.5"),
          "Updated com.carrotsearch:hppc to 0.9.5",
          "Updates com.carrotsearch:hppc from 0.9.1 to 0.9.5",
          labels = emptyList(),
        ),
        branchPrefix = "bazel-steward/com.carrotsearch/hppc/",
        commits = listOf(
          CommitRequest(
            "Updated com.carrotsearch:hppc to 0.9.5",
            listOf(FileChange(workspace.resolve("WORKSPACE"), 1024, 5, "0.9.5")),
          ),
        ),
        oldLibraries = listOf(MavenCoordinates.of("com.carrotsearch", "hppc", "0.9.1")),
      ),
      PullRequestSuggestion(
        NewPullRequest(
          GitBranch("bazel-steward/org.jline/jline/3.21.0"),
          "Updated org.jline:jline to 3.21.0",
          "Updates org.jline:jline from 3.13.1 to 3.21.0",
          labels = emptyList(),
        ),
        branchPrefix = "bazel-steward/org.jline/jline/",
        commits = listOf(
          CommitRequest(
            "Updated org.jline:jline to 3.21.0",
            listOf(FileChange(workspace.resolve("WORKSPACE"), 1057, 6, "3.21.0")),
          ),
        ),
        oldLibraries = listOf(MavenCoordinates.of("org.jline", "jline", "3.13.1")),
      ),
      PullRequestSuggestion(
        NewPullRequest(
          GitBranch("bazel-steward/jackson-core/2.15.0"),
          "Update jackson-core to 2.15.0",
          """Updated:
            |com.fasterxml.jackson.core:jackson-annotations from 2.14.2 to 2.15.0
            |com.fasterxml.jackson.core:jackson-core from 2.14.2 to 2.15.0
            |com.fasterxml.jackson.core:jackson-databind from 2.14.2 to 2.15.0
          """.trimMargin(),
          labels = emptyList(),
        ),
        branchPrefix = "bazel-steward/jackson-core/",
        commits = listOf(
          CommitRequest(
            "Update jackson-core to 2.15.0",
            listOf(
              FileChange(workspace.resolve("WORKSPACE"), 1122, 6, "2.15.0"),
              FileChange(workspace.resolve("WORKSPACE"), 1180, 6, "2.15.0"),
              FileChange(workspace.resolve("WORKSPACE"), 1242, 6, "2.15.0"),
            ),
          ),
        ),
        oldLibraries = listOf(
          MavenCoordinates.of("com.fasterxml.jackson.core", "jackson-annotations", "2.14.2"),
          MavenCoordinates.of("com.fasterxml.jackson.core", "jackson-core", "2.14.2"),
          MavenCoordinates.of("com.fasterxml.jackson.core", "jackson-databind", "2.14.2"),
        ),
      ),
      PullRequestSuggestion(
        NewPullRequest(
          GitBranch("bazel-steward/jackson/2.15.0"),
          "Update jackson to 2.15.0",
          """Updated:
            |com.fasterxml.jackson.dataformat:jackson-dataformat-yaml from 2.14.2 to 2.15.0
            |com.fasterxml.jackson.datatype:jackson-datatype-guava from 2.14.2 to 2.15.0
            |com.fasterxml.jackson.datatype:jackson-datatype-jdk8 from 2.14.2 to 2.15.0
            |com.fasterxml.jackson.datatype:jackson-datatype-jsr310 from 2.14.2 to 2.15.0
          """.trimMargin(),
          labels = emptyList(),
        ),
        branchPrefix = "bazel-steward/jackson/",
        commits = listOf(
          CommitRequest(
            "Update jackson to 2.15.0",
            listOf(
              FileChange(workspace.resolve("WORKSPACE"), 1317, 6, "2.15.0"),
              FileChange(workspace.resolve("WORKSPACE"), 1389, 6, "2.15.0"),
              FileChange(workspace.resolve("WORKSPACE"), 1460, 6, "2.15.0"),
              FileChange(workspace.resolve("WORKSPACE"), 1533, 6, "2.15.0"),
            ),
          ),
        ),
        oldLibraries = listOf(
          MavenCoordinates.of("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml", "2.14.2"),
          MavenCoordinates.of("com.fasterxml.jackson.datatype", "jackson-datatype-guava", "2.14.2"),
          MavenCoordinates.of("com.fasterxml.jackson.datatype", "jackson-datatype-jdk8", "2.14.2"),
          MavenCoordinates.of("com.fasterxml.jackson.datatype", "jackson-datatype-jsr310", "2.14.2"),
        ),
      ),
      PullRequestSuggestion(
        NewPullRequest(
          GitBranch("bazel-steward/glassfish/2.40.0"),
          "Updated glassfish to 2.40.0",
          "Updates glassfish from 2.39.1 to 2.40.0",
          labels = emptyList(),
        ),
        branchPrefix = "bazel-steward/glassfish/",
        commits = listOf(
          CommitRequest(
            "Updated glassfish to 2.40.0",
            listOf(
              FileChange(workspace.resolve("WORKSPACE"), 951, 6, "2.40.0"),
            ),
          ),
        ),
        oldLibraries = listOf(
          MavenCoordinates.of("org.glassfish.jersey.containers", "jersey-container-grizzly2-http", "2.39.1"),
          MavenCoordinates.of("org.glassfish.jersey.core", "jersey-client", "2.39.1"),
          MavenCoordinates.of("org.glassfish.jersey.core", "jersey-common", "2.39.1"),
          MavenCoordinates.of("org.glassfish.jersey.core", "jersey-server", "2.39.1"),
          MavenCoordinates.of("org.glassfish.jersey.inject", "jersey-hk2", "2.39.1"),
          MavenCoordinates.of("org.glassfish.jersey.media", "jersey-media-json-jackson", "2.39.1"),
        ),
      ),
    )

    prSuggestions shouldContainExactly expectedSuggestions
  }
}
