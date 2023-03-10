package org.virtuslab.bazelsteward.e2e.rules_update

import org.junit.jupiter.api.Disabled
import org.virtuslab.bazelsteward.e2e.E2EBase
import org.virtuslab.bazelsteward.e2e.RulesUpdateTest

@Disabled("Multiple sha256 in descriptions of newest releases")
class PythonRulesUpdateTest : RulesUpdateTest(
  "rules/trivial/rules_python",
  E2EBase().expectedBranches(
    "rules_python" to "0.19.0",
    "rules_jvm_external" to "4.5"
  )
)
