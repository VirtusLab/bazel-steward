package org.virtuslab.bazelsteward.e2e.rules_update

import org.junit.jupiter.api.Disabled
import org.virtuslab.bazelsteward.e2e.RulesUpdate

@Disabled("Scala's release's tag is not compatible with SemVer")
class ScalaRulesUpdateTest : RulesUpdate(
  "rules/trivial/rules_scala",
  arrayOf(
    "io_bazel_rules_scala" to "v5.0.0",
    "bazel-skylib" to "1.4.1"
  )
)
