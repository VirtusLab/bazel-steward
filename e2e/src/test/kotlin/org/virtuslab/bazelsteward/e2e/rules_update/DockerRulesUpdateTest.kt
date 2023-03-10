package org.virtuslab.bazelsteward.e2e.rules_update

import org.virtuslab.bazelsteward.e2e.E2EBase
import org.virtuslab.bazelsteward.e2e.RulesUpdateTest

class DockerRulesUpdateTest : RulesUpdateTest(
  "rules/trivial/rules_docker",
  E2EBase().expectedBranches(
    "rules_docker" to "v0.25.0",
    "rules_jvm_external" to "4.5"
  )
)
