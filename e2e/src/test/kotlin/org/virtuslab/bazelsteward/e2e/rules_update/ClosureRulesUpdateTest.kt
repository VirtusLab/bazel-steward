package org.virtuslab.bazelsteward.e2e.rules_update

import org.junit.jupiter.api.Disabled
import org.virtuslab.bazelsteward.e2e.E2EBase
import org.virtuslab.bazelsteward.e2e.RulesUpdateTest

@Disabled("No sha in description of newer releases.")
class ClosureRulesUpdateTest : RulesUpdateTest(
  "rules/trivial/rules_closure",
  E2EBase().expectedBranches(
    "rules_closure" to "0.12.0",
    "rules_jvm_external" to "4.5"
  )
)
