package org.virtuslab.bazelsteward.core.rules

import org.virtuslab.bazelsteward.core.library.Version

interface RulesResolver {
  fun resolveRuleVersions(ruleId: BazelRuleLibraryId): Map<BazelRuleLibraryId, Version>
}
