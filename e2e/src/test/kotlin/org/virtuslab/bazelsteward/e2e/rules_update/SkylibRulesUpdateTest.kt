package org.virtuslab.bazelsteward.e2e.rules_update

import org.junit.jupiter.api.Disabled
import org.virtuslab.bazelsteward.e2e.RulesUpdate

@Disabled("Multiple sha256 in descriptions of newest releases")
class SkylibRulesUpdateTest : RulesUpdate(
  "rules/trivial/rules_skylib",
  "bazel-skylib" to "1.4.1"
)
