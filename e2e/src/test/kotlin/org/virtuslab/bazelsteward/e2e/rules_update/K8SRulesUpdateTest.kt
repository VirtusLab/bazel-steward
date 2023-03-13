package org.virtuslab.bazelsteward.e2e.rules_update

import org.virtuslab.bazelsteward.e2e.RulesUpdate

class K8SRulesUpdateTest : RulesUpdate(
  "rules/trivial/rules_k8s",
  "rules_k8s" to "v0.7"
)
