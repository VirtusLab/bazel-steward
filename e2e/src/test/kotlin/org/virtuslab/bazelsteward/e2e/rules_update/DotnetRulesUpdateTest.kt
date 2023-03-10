package org.virtuslab.bazelsteward.e2e.rules_update

import org.virtuslab.bazelsteward.e2e.E2EBase
import org.virtuslab.bazelsteward.e2e.RulesUpdateTest

class DotnetRulesUpdateTest: RulesUpdateTest("rules/trivial/rules_dotnet",
  E2EBase().expectedBranches(
    "rules_apple" to "2.1.0",
    "rules_jvm_external" to "4.5"
  )
)
