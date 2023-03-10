package org.virtuslab.bazelsteward.e2e.rules_update

import org.virtuslab.bazelsteward.e2e.E2EBase
import org.virtuslab.bazelsteward.e2e.RulesUpdateTest

class JvmExternalRulesUpdateTest : RulesUpdateTest(
  "rules/trivial/rules_jvm_external",
  E2EBase().expectedBranches(
    "rules_jvm_external" to "4.5"
  )
)