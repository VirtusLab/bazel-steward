package org.virtuslab.bazelsteward.e2e.rules_update

import org.junit.jupiter.api.Disabled
import org.virtuslab.bazelsteward.e2e.E2EBase
import org.virtuslab.bazelsteward.e2e.RulesUpdateTest

@Disabled("All releases are actually prereleases.")
class ProtoRulesUpdateTest : RulesUpdateTest(
  "rules/trivial/rules_proto",
  E2EBase().expectedBranches(
    "rules_proto" to "5.3.0-21.7",
    "rules_jvm_external" to "4.5"
  )
)
