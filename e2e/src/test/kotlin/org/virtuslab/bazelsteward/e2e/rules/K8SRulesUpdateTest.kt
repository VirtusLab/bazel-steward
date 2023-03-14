package org.virtuslab.bazelsteward.e2e.rules

import org.virtuslab.bazelsteward.e2e.rules.fixture.RulesUpdate

class K8SRulesUpdateTest : RulesUpdate(
  "rules/rules_k8s",
  "rules_k8s" to "v0.7"
)
