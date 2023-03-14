package org.virtuslab.bazelsteward.e2e.rules

import org.junit.jupiter.api.Disabled
import org.virtuslab.bazelsteward.e2e.rules.fixture.RulesUpdate

@Disabled("Multiple sha256 in descriptions of newest releases")
class SkylibRulesUpdateTest : RulesUpdate(
  "rules/rules_skylib",
  "bazel-skylib" to "1.4.1"
)
