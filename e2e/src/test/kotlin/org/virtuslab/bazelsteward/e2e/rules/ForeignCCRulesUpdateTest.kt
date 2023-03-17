package org.virtuslab.bazelsteward.e2e.rules

import org.virtuslab.bazelsteward.e2e.rules.fixture.RulesUpdate

class ForeignCCRulesUpdateTest : RulesUpdate(
  "rules/rules_foreign_cc",
  "rules_foreign_cc" to "0.9.0"
)
