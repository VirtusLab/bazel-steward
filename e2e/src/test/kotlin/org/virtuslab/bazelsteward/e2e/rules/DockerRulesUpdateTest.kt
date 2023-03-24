package org.virtuslab.bazelsteward.e2e.rules

import org.virtuslab.bazelsteward.e2e.rules.fixture.RulesUpdate

class DockerRulesUpdateTest : RulesUpdate(
  "rules/rules_docker",
  "rules_docker" to "v0.25.0",
)
