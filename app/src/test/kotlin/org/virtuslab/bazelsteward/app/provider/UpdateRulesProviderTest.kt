package org.virtuslab.bazelsteward.app.provider

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.virtuslab.bazelsteward.config.repo.DependencyNameFilter
import org.virtuslab.bazelsteward.config.repo.UpdateRulesConfig
import org.virtuslab.bazelsteward.core.common.PinningStrategy
import org.virtuslab.bazelsteward.fixture.DependencyKindsFixture

class UpdateRulesProviderTest {

  private val dependencyKinds = DependencyKindsFixture()

  @Test
  fun `should disable selected kind and keep others`() {
    val provider = UpdateRulesProvider(
      listOf(
        UpdateRulesConfig(
          kinds = listOf("maven"),
          enabled = false,
        ),
      ),
      dependencyKinds.all,
    )

    provider.isKindEnabled(dependencyKinds.maven) shouldBe false
    provider.isKindEnabled(dependencyKinds.bazelVersion) shouldBe true
    provider.isKindEnabled(dependencyKinds.bazelRules) shouldBe true
  }

  @Test
  fun `should disable selected kind even if there is a more specific rule, but not explicitly enabled`() {
    val provider = UpdateRulesProvider(
      listOf(
        UpdateRulesConfig(
          kinds = listOf("maven"),
          enabled = null,
          dependencies = listOf(DependencyNameFilter.parse("org.apache.maven:maven-core")),
          pin = PinningStrategy.parse("1.2."),
        ),
        UpdateRulesConfig(
          kinds = listOf("maven"),
          enabled = false,
        ),
      ),
      dependencyKinds.all,
    )

    provider.isKindEnabled(dependencyKinds.maven) shouldBe false
  }

  @Test
  fun `should not disable selected kind if any more specific, explicitly enabled rule exists`() {
    val provider = UpdateRulesProvider(
      listOf(
        UpdateRulesConfig(
          kinds = listOf("maven"),
          enabled = true,
          dependencies = listOf(DependencyNameFilter.parse("org.apache.maven:maven-core")),
          pin = PinningStrategy.parse("1.2."),
        ),
        UpdateRulesConfig(
          kinds = listOf("maven"),
          enabled = false,
        ),
      ),
      dependencyKinds.all,
    )

    provider.isKindEnabled(dependencyKinds.maven) shouldBe true
    provider.isKindEnabled(dependencyKinds.bazelVersion) shouldBe true
    provider.isKindEnabled(dependencyKinds.bazelRules) shouldBe true
  }
}
