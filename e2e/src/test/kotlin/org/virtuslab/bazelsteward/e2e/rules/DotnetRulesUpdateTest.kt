package org.virtuslab.bazelsteward.e2e.rules

import org.virtuslab.bazelsteward.e2e.rules.fixture.RulesUpdate

class DotnetRulesUpdateTest : RulesUpdate(
  "rules/rules_dotnet",
  "rules_dotnet" to "v0.8.9",
)
