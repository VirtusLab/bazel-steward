package org.virtuslab.bazelsteward.e2e.rules_update

import org.virtuslab.bazelsteward.e2e.RulesUpdate

class DockerRulesUpdateTest : RulesUpdate(
  "rules/trivial/rules_docker",
  "rules_docker" to "v0.25.0"
)
