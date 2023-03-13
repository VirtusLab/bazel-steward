package org.virtuslab.bazelsteward.e2e.rules_update

import org.virtuslab.bazelsteward.e2e.RulesUpdate

class GoRulesUpdateTest : RulesUpdate(
  "rules/trivial/rules_go",
  "rules_go" to "v0.38.1"
)
