package org.virtuslab.bazelsteward.e2e.rules.heuristic

import org.virtuslab.bazelsteward.e2e.rules.fixture.BazelRulesHeuristicUpdate

class HeuristicUpdateMultipleUrlsVariablesTest : BazelRulesHeuristicUpdate(
  project = "rules/heuristic/heuristic_rules_multiple_urls_variables",
  expectedUrl = "https://github.com/bazelbuild/rules_closure/archive/0.12.0.tar.gz",
  expectedSha256 = "7d206c2383811f378a5ef03f4aacbcf5f47fd8650f6abbc3fa89f3a27ddcccc",
  expectedVersion = "0.12.0",
  expectedBranch = "rules_closure",
)
