package org.virtuslab.bazelsteward.e2e.rules_update

import org.virtuslab.bazelsteward.e2e.E2EBase
import org.virtuslab.bazelsteward.e2e.RulesUpdateTest

class CCRulesUpdateTest : RulesUpdateTest(
  "rules/trivial/rules_cc",
  E2EBase().expectedBranches(
    "rules_cc" to "0.0.6",
    "rules_jvm_external" to "4.5"
  )
)
