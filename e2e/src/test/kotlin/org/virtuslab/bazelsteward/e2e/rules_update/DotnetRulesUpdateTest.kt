package org.virtuslab.bazelsteward.e2e.rules_update

import org.virtuslab.bazelsteward.e2e.RulesUpdate

class DotnetRulesUpdateTest : RulesUpdate(
  "rules/trivial/rules_dotnet",
  arrayOf(
    "rules_dotnet" to "v0.8.7"
  )
)
