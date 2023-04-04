package org.virtuslab.bazelsteward.e2e.rules

import org.virtuslab.bazelsteward.e2e.rules.fixture.BazelRulesHeuristicUpdate

class HeuristicUpdateMultipleUrlsVariablesTest : BazelRulesHeuristicUpdate(
  "rules/heuristic_rules_multiple_urls_variables",
  "https://github.com/bazelbuild/rules_closure/archive/0.12.0.tar.gz",
  "7d206c2383811f378a5ef03f4aacbcf5f47fd8650f6abbc3fa89f3a27ddcccc",
  "0.12.0",
)
