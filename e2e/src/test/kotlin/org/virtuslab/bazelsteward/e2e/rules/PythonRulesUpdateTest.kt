package org.virtuslab.bazelsteward.e2e.rules

import org.junit.jupiter.api.Disabled
import org.virtuslab.bazelsteward.e2e.rules.fixture.RulesUpdate

@Disabled("Multiple sha256 in descriptions of newest releases")
class PythonRulesUpdateTest : RulesUpdate(
  "rules/rules_python",
  "rules_python" to "0.19.0"
)
