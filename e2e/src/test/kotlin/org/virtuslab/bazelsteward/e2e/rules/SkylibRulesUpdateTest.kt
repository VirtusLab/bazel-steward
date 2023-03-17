package org.virtuslab.bazelsteward.e2e.rules

import org.virtuslab.bazelsteward.e2e.rules.fixture.RulesUpdate

class SkylibRulesUpdateTest : RulesUpdate(
  "rules/rules_skylib",
  "bazel-skylib" to "1.4.1",
)
