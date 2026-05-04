package org.virtuslab.bazelsteward.e2e.rules

import org.virtuslab.bazelsteward.e2e.rules.fixture.RulesUpdate

class JvmExternalRulesUpdateTest : RulesUpdate(
  "rules/rules_jvm_external",
  "rules_jvm_external" to "5.1",
  expectedUrl = "https://github.com/bazel-contrib/rules_jvm_external/releases/download/5.1/rules_jvm_external-5.1.tar.gz",
  expectedSha256 = "8c3b207722e5f97f1c83311582a6c11df99226e65e2471086e296561e57cc954",
)
