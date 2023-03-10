package org.virtuslab.bazelsteward.e2e.rules_update

import org.virtuslab.bazelsteward.e2e.RulesUpdate

class JavaRulesUpdateTest : RulesUpdate(
  "rules/trivial/rules_java",
  arrayOf(
    "rules_java" to "5.4.1"
  )
)
