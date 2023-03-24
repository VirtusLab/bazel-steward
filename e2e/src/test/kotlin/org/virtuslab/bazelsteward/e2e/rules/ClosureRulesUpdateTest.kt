package org.virtuslab.bazelsteward.e2e.rules

import org.junit.jupiter.api.Disabled
import org.virtuslab.bazelsteward.e2e.rules.fixture.RulesUpdate

@Disabled("No sha in description of newer releases.")
class ClosureRulesUpdateTest : RulesUpdate(
  "rules/rules_closure",
  "rules_closure" to "0.12.0",
)
