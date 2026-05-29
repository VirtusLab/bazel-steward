package org.virtuslab.bazelsteward.action

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ReleaseTagResolverTest {
  private val taggedCommitSha = "15ba5fa2b7eb9d9f2e67edb8cb355130b96d7a4d"
  private val otherCommitSha = "cccccccccccccccccccccccccccccccccccccccc"
  private val fakeReleases = listOf(
    "v1.7.2-rc9\tRC",
    "v1.7.2\tRelease 1.7.2",
    "v1.7.2.1\tPatch",
    "v1.7.3\tRelease 1.7.3",
  )
  private val fakeTags = listOf(
    RepositoryTag("v1.7.2", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"),
    RepositoryTag("v1.7.2.1", taggedCommitSha),
    RepositoryTag("v1.7.3", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"),
    RepositoryTag("v1.7.2-rc9", taggedCommitSha),
  )
  private val fakeGh = FakeMetadataProvider(
    releases = fakeReleases,
    tags = fakeTags,
    refs = mapOf("main" to taggedCommitSha, "develop" to otherCommitSha),
  )

  @Test
  fun `v prefix resolves to latest matching stable release`() {
    ReleaseTagResolver.resolveLatestMatchingRelease("v1.7", fakeReleases) shouldBe "v1.7.3"
  }

  @Test
  fun `exact v tag ref resolves to latest matching patch release`() {
    ReleaseTagResolver.resolveLatestMatchingRelease("v1.7.2", fakeReleases) shouldBe "v1.7.2.1"
  }

  @Test
  fun `commit SHA resolves to release tag pointing to the same commit`() {
    ReleaseTagResolver.resolve(
      taggedCommitSha,
      "VirtusLab/bazel-steward",
      fakeGh,
    ) shouldBe "v1.7.2.1"
  }

  @Test
  fun `branch ref resolves to release tag via branch head commit`() {
    ReleaseTagResolver.resolve("main", "VirtusLab/bazel-steward", fakeGh) shouldBe "v1.7.2.1"
  }

  @Test
  fun `unknown branch ref fails`() {
    shouldThrow<IllegalStateException> {
      ReleaseTagResolver.resolve("non-existing-branch", "VirtusLab/bazel-steward", fakeGh)
    }
  }

  @Test
  fun `commit SHA without matching release tag fails`() {
    shouldThrow<IllegalStateException> {
      ReleaseTagResolver.resolve(
        otherCommitSha,
        "VirtusLab/bazel-steward",
        fakeGh,
      )
    }
  }

  @Test
  fun `matchesTagPattern accepts optional numeric suffix segments`() {
    ReleaseTagResolver.matchesTagPattern("v1.7.2.1", "v1.7.2") shouldBe true
    ReleaseTagResolver.matchesTagPattern("v1.7.2-rc9", "v1.7.2") shouldBe false
    ReleaseTagResolver.matchesTagPattern("v1.7.3", "v1.7") shouldBe true
  }

  @Test
  fun `short commit SHA resolves to release tag`() {
    val shortSha = taggedCommitSha.take(12)
    ReleaseTagResolver.resolve(shortSha, "VirtusLab/bazel-steward", fakeGh) shouldBe "v1.7.2.1"
  }

  @Test
  fun `ref resolving to commit without release tag fails`() {
    shouldThrow<IllegalStateException> {
      ReleaseTagResolver.resolve("develop", "VirtusLab/bazel-steward", fakeGh)
    }
  }

  @Test
  fun `commit SHA can resolve to rc release tag`() {
    val rcOnlyGh = FakeMetadataProvider(
      releases = listOf("v1.7.2-rc9\tRC"),
      tags = listOf(RepositoryTag("v1.7.2-rc9", taggedCommitSha)),
      refs = mapOf("main" to taggedCommitSha),
    )
    ReleaseTagResolver.resolve(
      taggedCommitSha,
      "VirtusLab/bazel-steward",
      rcOnlyGh,
    ) shouldBe "v1.7.2-rc9"
  }

  private class FakeMetadataProvider(
    private val releases: List<String>,
    private val tags: List<RepositoryTag>,
    private val refs: Map<String, String>,
  ) : GhReleaseMetadataProvider {
    override fun listReleases(repository: String): List<String> = releases

    override fun listTags(repository: String): List<RepositoryTag> = tags

    override fun resolveRefToCommitSha(repository: String, ref: String): String? = refs[ref]
  }
}
