package org.virtuslab.bazelsteward.e2e.rules_update

import org.junit.jupiter.api.Disabled
import org.virtuslab.bazelsteward.e2e.RulesUpdate

@Disabled("Multiple sha256 in descriptions of newest releases")
class PythonRulesUpdateTest : RulesUpdate(
  "rules/trivial/rules_python",
  arrayOf(
    "rules_python" to "0.19.0"
  )
)
