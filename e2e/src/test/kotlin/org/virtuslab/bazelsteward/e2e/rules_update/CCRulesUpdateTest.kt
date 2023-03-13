package org.virtuslab.bazelsteward.e2e.rules_update

import org.virtuslab.bazelsteward.e2e.RulesUpdate

class CCRulesUpdateTest : RulesUpdate(
  "rules/trivial/rules_cc",
  "rules_cc" to "0.0.6"
)
