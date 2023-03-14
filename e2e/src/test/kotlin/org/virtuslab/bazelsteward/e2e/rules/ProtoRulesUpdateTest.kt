package org.virtuslab.bazelsteward.e2e.rules

import org.junit.jupiter.api.Disabled
import org.virtuslab.bazelsteward.e2e.rules.fixture.RulesUpdate

@Disabled("All releases are actually prereleases.")
class ProtoRulesUpdateTest : RulesUpdate(
  "rules/rules_proto",
  "rules_proto" to "5.3.0-21.7"
)
