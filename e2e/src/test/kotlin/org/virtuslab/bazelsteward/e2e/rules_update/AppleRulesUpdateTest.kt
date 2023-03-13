package org.virtuslab.bazelsteward.e2e.rules_update

import org.virtuslab.bazelsteward.e2e.RulesUpdate

class AppleRulesUpdateTest : RulesUpdate(
  "rules/trivial/rules_apple",
  "rules_apple" to "2.1.0"
)
