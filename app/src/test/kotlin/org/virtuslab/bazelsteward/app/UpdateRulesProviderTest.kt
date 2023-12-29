package org.virtuslab.bazelsteward.app

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.kohsuke.github.GitHub
import org.virtuslab.bazelsteward.app.provider.UpdateRulesProvider
import org.virtuslab.bazelsteward.bazel.rules.BazelRulesDependencyKind
import org.virtuslab.bazelsteward.bazel.rules.BazelRulesExtractor
import org.virtuslab.bazelsteward.bazel.rules.GithubRulesResolver
import org.virtuslab.bazelsteward.bazel.version.BazelUpdater
import org.virtuslab.bazelsteward.bazel.version.BazelVersionDependencyKind
import org.virtuslab.bazelsteward.bzlmod.BzlModDataExtractor
import org.virtuslab.bazelsteward.bzlmod.BzlModDependencyKind
import org.virtuslab.bazelsteward.bzlmod.BzlModRepository
import org.virtuslab.bazelsteward.config.repo.RepoConfigParser
import org.virtuslab.bazelsteward.core.DependencyKind
import org.virtuslab.bazelsteward.core.common.PinningStrategy
import org.virtuslab.bazelsteward.core.library.BumpingStrategy
import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.VersioningSchema
import org.virtuslab.bazelsteward.maven.MavenCoordinates
import org.virtuslab.bazelsteward.maven.MavenDataExtractor
import org.virtuslab.bazelsteward.maven.MavenDependencyKind
import org.virtuslab.bazelsteward.maven.MavenRepository
import java.nio.file.Paths

class UpdateRulesProviderTest {

  @Test
  fun `should apply custom default bumping`() {
    val library = MavenCoordinates.of("org.virtuslab.ideprobe", "driver_2.13", "0.14.0")

    val result = rulesProvider.resolveForLibrary(library)

    result.bumpingStrategy shouldBe BumpingStrategy.Minimal
  }

  @Test
  fun `should apply custom versioning from kind`() {
    val library = MavenCoordinates.of("org.virtuslab.ideprobe", "driver_2.13", "0.14.0")

    val result = rulesProvider.resolveForLibrary(library)

    result.versioningSchema shouldBe VersioningSchema.SemVer
  }

  @Test
  fun `should apply custom pinning and bumping for list of dependencies`() {
    val depA = MavenCoordinates.of("org.virtuslab", "dep-a", "1.0.0")
    val depB = MavenCoordinates.of("org.virtuslab", "dep-b", "1.0.0")
    val depAbc = MavenCoordinates.of("org.virtuslab", "dep-abc", "1.0.0")
    val protobuf = MavenCoordinates.of("com.google", "protobuf", "27")
    val guava = MavenCoordinates.of("com.google", "guava", "28")
    val android = MavenCoordinates.of("com.google.android", "sdk", "17")

    val configuredPinning = PinningStrategy.Prefix("1.2.")
    val configuredBumping = BumpingStrategy.Latest

    fun shouldApplyCustomRule(library: Library) {
      val rules = rulesProvider.resolveForLibrary(library)
      rules.pinningStrategy shouldBe configuredPinning
      rules.bumpingStrategy shouldBe configuredBumping
      rules.versioningSchema shouldBe VersioningSchema.SemVer
    }

    fun shouldApplyGeneralRules(library: Library) {
      val rules = rulesProvider.resolveForLibrary(library)
      rules.pinningStrategy shouldBe PinningStrategy.None
      rules.bumpingStrategy shouldBe BumpingStrategy.Minimal
      rules.versioningSchema shouldBe VersioningSchema.SemVer
    }

    shouldApplyCustomRule(depA)
    shouldApplyCustomRule(depB)
    shouldApplyCustomRule(protobuf)
    shouldApplyCustomRule(guava)

    shouldApplyGeneralRules(depAbc)
    shouldApplyGeneralRules(android)
  }

  private fun createDependencyKinds(): List<DependencyKind<*>> {
    val workspaceRoot = Paths.get(".")
    return listOf(
      BazelVersionDependencyKind(BazelUpdater()),
      MavenDependencyKind(MavenDataExtractor(workspaceRoot), MavenRepository()),
      BzlModDependencyKind(BzlModDataExtractor(workspaceRoot), BzlModRepository()),
      BazelRulesDependencyKind(BazelRulesExtractor(), GithubRulesResolver(GitHub.connectAnonymously())),
    )
  }

  private val config = RepoConfigParser().parse(this::class.java.classLoader.getResource("example-config.yaml")!!.readText())
  private val rulesProvider = UpdateRulesProvider(config.updateRules, createDependencyKinds())
}
