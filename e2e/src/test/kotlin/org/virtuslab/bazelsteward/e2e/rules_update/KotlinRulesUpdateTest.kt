package org.virtuslab.bazelsteward.e2e.rules_update

import org.virtuslab.bazelsteward.e2e.RulesUpdate

class KotlinRulesUpdateTest : RulesUpdate(
  "rules/trivial/rules_kotlin",
  "rules_kotlin" to "v1.7.1"
)
