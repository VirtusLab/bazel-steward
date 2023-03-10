package org.virtuslab.bazelsteward.e2e.rules_update

import org.junit.jupiter.api.Disabled
import org.virtuslab.bazelsteward.e2e.E2EBase
import org.virtuslab.bazelsteward.e2e.RulesUpdateTest

class K8SRulesUpdateTest : RulesUpdateTest(
  "rules/trivial/rules_k8s",
  E2EBase().expectedBranches(
    "rules_k8s" to "v0.7",
    "rules_jvm_external" to "4.5"
  )
)
