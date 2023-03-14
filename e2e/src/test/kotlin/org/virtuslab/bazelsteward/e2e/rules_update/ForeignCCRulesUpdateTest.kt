package org.virtuslab.bazelsteward.e2e.rules_update

import org.virtuslab.bazelsteward.e2e.RulesUpdate

class ForeignCCRulesUpdateTest : RulesUpdate(
  "rules/trivial/rules_foreign_cc",
  "rules_foreign_cc" to "0.9.0"
)
