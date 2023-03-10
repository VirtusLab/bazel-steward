package org.virtuslab.bazelsteward.e2e.rules_update

import org.virtuslab.bazelsteward.e2e.E2EBase
import org.virtuslab.bazelsteward.e2e.RulesUpdate

class JavaRulesUpdateTest : RulesUpdate(
  "rules/trivial/rules_java",
  E2EBase().expectedBranches(
    "rules_java" to "5.4.1",
    "rules_jvm_external" to "4.5"
  )
)
