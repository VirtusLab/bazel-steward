package org.virtuslab.bazelsteward.e2e.rules

import org.virtuslab.bazelsteward.e2e.rules.fixture.RulesUpdate

class CCRulesUpdateTest : RulesUpdate(
  "rules/rules_cc",
  "rules_cc" to "0.0.6",
)
