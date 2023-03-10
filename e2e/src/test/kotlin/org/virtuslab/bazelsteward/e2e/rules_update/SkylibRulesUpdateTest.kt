package org.virtuslab.bazelsteward.e2e.rules_update

import org.virtuslab.bazelsteward.e2e.E2EBase
import org.virtuslab.bazelsteward.e2e.RulesUpdateTest

class SkylibRulesUpdateTest: RulesUpdateTest("rules/trivial/rules_skylib",
  E2EBase().expectedBranches(
    "bazel-skylib" to "1.3.0",
    "io_bazel_rules_scala" to "v5.0.0",
    "rules_jvm_external" to "4.5"
  )
)
