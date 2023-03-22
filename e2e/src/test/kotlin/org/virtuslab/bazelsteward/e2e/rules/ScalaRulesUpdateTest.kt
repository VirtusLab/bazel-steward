package org.virtuslab.bazelsteward.e2e.rules

import org.junit.jupiter.api.Disabled
import org.virtuslab.bazelsteward.e2e.rules.fixture.RulesUpdate

@Disabled("Scala's release's tag is not compatible with SemVer")
class ScalaRulesUpdateTest : RulesUpdate(
  "rules/rules_scala",
  "io_bazel_rules_scala" to "v5.0.0",
  "bazel-skylib" to "1.4.1",
)
