package org.virtuslab.bazelsteward.app

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.virtuslab.bazelsteward.app.provider.PullRequestConfigProvider
import org.virtuslab.bazelsteward.config.repo.PullRequestsConfig
import org.virtuslab.bazelsteward.core.common.UpdateSuggestion
import org.virtuslab.bazelsteward.core.library.SimpleVersion
import org.virtuslab.bazelsteward.core.replacement.LibraryUpdate
import org.virtuslab.bazelsteward.maven.MavenCoordinates
import org.virtuslab.bazelsteward.maven.MavenLibraryId

class PullRequestSuggesterTest {

  @Test
  fun `should fill placeholder values with artifact data`() {
    val config = PullRequestsConfig(
      title = "\${group} and \${artifact}",
      body = "\${dependencyId} update \${versionFrom} to \${versionTo}, also \${not-existing}",
      labels = listOf("test-label"),
    )

    val provider = PullRequestConfigProvider(listOf(config), emptyList())

    val group = "group-name"
    val artifact = "artefact-name"
    val versionFrom = "version-test-old"
    val versionTo = "version-test-new"

    val prSuggester = PullRequestSuggester(provider)
    val mavenLibraryId = MavenLibraryId(group, artifact)
    val mavenCoordinates = MavenCoordinates(mavenLibraryId, SimpleVersion(versionFrom))
    val libraryUpdate = LibraryUpdate(UpdateSuggestion(mavenCoordinates, SimpleVersion(versionTo)), emptyList())

    val dependencyId = mavenLibraryId.name

    val newPr = prSuggester.suggestPullRequests(listOf(libraryUpdate)).single()
    val dsc = newPr.description

    dsc.title shouldBe "$group and $artifact"
    dsc.body shouldBe "$dependencyId update $versionFrom to $versionTo, also \${not-existing}"
    dsc.labels.shouldContainExactlyInAnyOrder("test-label")
  }

  @Test
  fun `should provide default template`() {
    val provider = PullRequestConfigProvider(emptyList(), emptyList())

    val group = "group-name"
    val artifact = "artefact-name"
    val versionFrom = "version-test-old"
    val versionTo = "version-test-new"

    val prSuggester = PullRequestSuggester(provider)
    val mavenLibraryId = MavenLibraryId(group, artifact)
    val mavenCoordinates = MavenCoordinates(mavenLibraryId, SimpleVersion(versionFrom))
    val libraryUpdate = LibraryUpdate(UpdateSuggestion(mavenCoordinates, SimpleVersion(versionTo)), emptyList())

    val dependencyId = mavenLibraryId.name

    val newPr = prSuggester.suggestPullRequests(listOf(libraryUpdate))[0]
    val dsc = newPr.description

    dsc.title shouldBe "Updated $dependencyId to $versionTo"
    dsc.body shouldBe "Updates $dependencyId from $versionFrom to $versionTo"
    dsc.labels.shouldContainExactlyInAnyOrder(emptyList())
  }
}
