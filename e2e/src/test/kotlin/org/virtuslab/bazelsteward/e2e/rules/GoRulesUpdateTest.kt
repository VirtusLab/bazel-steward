package org.virtuslab.bazelsteward.e2e.rules

import org.virtuslab.bazelsteward.e2e.rules.fixture.RulesUpdate

class GoRulesUpdateTest : RulesUpdate(
  "rules/rules_go",
  "rules_go" to "v0.40.0",
)
