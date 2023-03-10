package org.virtuslab.bazelsteward.e2e.rules_update

import org.junit.jupiter.api.Disabled
import org.virtuslab.bazelsteward.e2e.E2EBase
import org.virtuslab.bazelsteward.e2e.RulesUpdateTest

@Disabled("Scala's release's tag is not compatible with SemVer")
class ScalaRulesUpdateTest : RulesUpdateTest(
  "rules/trivial/rules_scala",
  E2EBase().expectedBranches(
    "io_bazel_rules_scala" to "v5.0.0",
    "bazel-skylib" to "1.4.1",
    "rules_jvm_external" to "4.5"
  )
)