package org.virtuslab.bazelsteward.e2e.rules

import org.virtuslab.bazelsteward.e2e.rules.fixture.BazelRulesHeuristicUpdate

class HeuristicUpdateShaStringTest: BazelRulesHeuristicUpdate(
  "rules/heuristic_rules_sha_string",
  "https://github.com/bazelbuild/rules_apple/releases/download/2.2.0/rules_apple.2.2.0.tar.gz",
  "9e26307516c4d5f2ad4aee90ac01eb8cd31f9b8d6ea93619fc64b3cbc81b0944",
  "2.2.0",
)
