package org.virtuslab.bazelsteward.e2e.rules

import org.virtuslab.bazelsteward.e2e.rules.fixture.RulesUpdate

class KotlinRulesUpdateTest : RulesUpdate(
  "rules/rules_kotlin",
  "rules_kotlin" to "v1.8",
)
