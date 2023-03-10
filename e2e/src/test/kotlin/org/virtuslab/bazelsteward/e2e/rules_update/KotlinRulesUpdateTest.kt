package org.virtuslab.bazelsteward.e2e.rules_update

import org.virtuslab.bazelsteward.e2e.E2EBase
import org.virtuslab.bazelsteward.e2e.RulesUpdate

class KotlinRulesUpdateTest : RulesUpdate(
  "rules/trivial/rules_kotlin",
  E2EBase().expectedBranches(
    "rules_kotlin" to "v1.7.1",
    "rules_jvm_external" to "4.5"
  )
)
