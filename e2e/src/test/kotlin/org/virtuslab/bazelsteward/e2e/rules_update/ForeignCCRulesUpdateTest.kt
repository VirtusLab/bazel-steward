package org.virtuslab.bazelsteward.e2e.rules_update

import org.virtuslab.bazelsteward.e2e.E2EBase
import org.virtuslab.bazelsteward.e2e.RulesUpdate

class ForeignCCRulesUpdateTest : RulesUpdate(
  "rules/trivial/rules_foreign_cc",
  E2EBase().expectedBranches(
    "rules_foreign_cc" to "0.9.0",
    "rules_jvm_external" to "4.5"
  )
)
