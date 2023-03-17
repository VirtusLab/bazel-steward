package org.virtuslab.bazelsteward.e2e.rules

import org.virtuslab.bazelsteward.e2e.rules.fixture.RulesUpdate

class ClosureRulesUpdateTest : RulesUpdate(
  "rules/rules_closure",
  "rules_closure" to "0.12.0",
)
