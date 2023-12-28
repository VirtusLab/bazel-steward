package org.virtuslab.bazelsteward.bzlmod

data class BzlModData(
  val repositories: List<String>,
  val dependencies: List<BazelModule>,
) {
  fun filterNot(skip: (BazelModule) -> Boolean): BzlModData {
    return this.copy(dependencies = dependencies.filterNot(skip))
  }
}
