package org.virtuslab.bazelsteward.e2e.rules_update

import org.virtuslab.bazelsteward.e2e.E2EBase
import org.virtuslab.bazelsteward.e2e.RulesUpdateTest

class GoRulesUpdateTest : RulesUpdateTest(
  "rules/trivial/rules_go",
  E2EBase().expectedBranches(
    "rules_go" to "v0.38.1",
    "rules_jvm_external" to "4.5"
  )
)
