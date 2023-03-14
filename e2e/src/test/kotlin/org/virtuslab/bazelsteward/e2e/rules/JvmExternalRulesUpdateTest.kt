package org.virtuslab.bazelsteward.e2e.rules

import org.virtuslab.bazelsteward.e2e.rules.fixture.RulesUpdate

class JvmExternalRulesUpdateTest : RulesUpdate(
  "rules/rules_jvm_external",
  "rules_jvm_external" to "4.5"
)
