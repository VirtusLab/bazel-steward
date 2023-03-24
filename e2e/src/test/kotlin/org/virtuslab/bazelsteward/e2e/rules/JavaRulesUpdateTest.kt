package org.virtuslab.bazelsteward.e2e.rules

import org.virtuslab.bazelsteward.e2e.rules.fixture.RulesUpdate

class JavaRulesUpdateTest : RulesUpdate(
  "rules/rules_java",
  "rules_java" to "5.5.0",
)
