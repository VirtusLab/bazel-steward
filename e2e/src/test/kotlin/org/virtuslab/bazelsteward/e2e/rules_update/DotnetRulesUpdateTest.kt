package org.virtuslab.bazelsteward.e2e.rules_update

import org.virtuslab.bazelsteward.e2e.E2EBase
import org.virtuslab.bazelsteward.e2e.RulesUpdate

class DotnetRulesUpdateTest : RulesUpdate(
  "rules/trivial/rules_dotnet",
  E2EBase().expectedBranches(
    "rules_dotnet" to "v0.8.7",
    "rules_jvm_external" to "4.5"
  )
)
