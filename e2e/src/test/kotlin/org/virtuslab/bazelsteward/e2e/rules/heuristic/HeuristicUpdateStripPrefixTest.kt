package org.virtuslab.bazelsteward.e2e.rules.heuristic

import org.virtuslab.bazelsteward.e2e.rules.fixture.BazelRulesHeuristicUpdate

class HeuristicUpdateStripPrefixTest : BazelRulesHeuristicUpdate(
  "rules/heuristic/heuristic_rules_strip_prefix",
  "https://github.com/bazelbuild/rules_closure/archive/0.12.0.tar.gz",
  "7d206c2383811f378a5ef03f4aacbcf5f47fd8650f6abbc3fa89f3a27ddcccc",
  "0.12.0",
  "rules_closure",
)
