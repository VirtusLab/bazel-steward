package org.virtuslab.bazelsteward.e2e.rules

import org.virtuslab.bazelsteward.e2e.rules.fixture.RulesUpdate

class AppleRulesUpdateTest : RulesUpdate(
  "rules/rules_apple",
  "rules_apple" to "2.3.0",
)
