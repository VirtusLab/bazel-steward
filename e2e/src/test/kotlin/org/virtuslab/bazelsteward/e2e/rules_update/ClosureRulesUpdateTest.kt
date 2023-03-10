package org.virtuslab.bazelsteward.e2e.rules_update

import org.junit.jupiter.api.Disabled
import org.virtuslab.bazelsteward.e2e.E2EBase
import org.virtuslab.bazelsteward.e2e.RulesUpdate

@Disabled("No sha in description of newer releases.")
class ClosureRulesUpdateTest : RulesUpdate(
  "rules/trivial/rules_closure",
  E2EBase().expectedBranches(
    "rules_closure" to "0.12.0",
    "rules_jvm_external" to "4.5"
  )
)
