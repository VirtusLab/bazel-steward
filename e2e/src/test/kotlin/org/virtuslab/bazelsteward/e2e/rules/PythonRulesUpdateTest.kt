package org.virtuslab.bazelsteward.e2e.rules

import org.virtuslab.bazelsteward.e2e.rules.fixture.RulesUpdate

class PythonRulesUpdateTest : RulesUpdate(
  "rules/rules_python",
  "rules_python" to "0.20.0",
)
