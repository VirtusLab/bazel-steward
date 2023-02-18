package org.virtuslab.bazelsteward.bazel.rules

import org.virtuslab.bazelsteward.core.library.Version

interface RulesResolver {
  fun resolveRuleVersions(ruleId: RuleLibraryId): Map<RuleLibraryId, Version>
}
