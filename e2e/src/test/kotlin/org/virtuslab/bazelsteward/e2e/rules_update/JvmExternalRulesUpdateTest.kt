package org.virtuslab.bazelsteward.e2e.rules_update

import org.virtuslab.bazelsteward.e2e.RulesUpdate

class JvmExternalRulesUpdateTest : RulesUpdate(
  "rules/trivial/rules_jvm_external",
  "rules_jvm_external" to "4.5"
)
