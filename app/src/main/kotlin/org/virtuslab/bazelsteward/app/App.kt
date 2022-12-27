package org.virtuslab.bazelsteward.app

import arrow.core.flattenOption
import org.virtuslab.bazelsteward.common.GitClient

class App(private val ctx: Context) {
  suspend fun run() {
    ctx.gitClient.checkoutBaseBranch()
    val definitions = ctx.bazelFileSearch.buildDefinitions
    val currentDependencies = ctx.mavenDependencyExtractor.extract()
    val availableVersions = ctx.mavenRepository.findVersions(currentDependencies)
    val updateSuggestions =
      availableVersions.map {
        ctx.updateLogic.selectUpdate(it.key, it.value)
      }.flattenOption()
    val changeSuggestions = ctx.fileUpdateSearch.searchBuildFiles(definitions, updateSuggestions)
    changeSuggestions.forEach { change ->
      val branch = GitClient.Companion.fileChangeSuggestionToBranch(change)
      if (!ctx.gitHostClient.checkIfPrExists(branch)) {
        ctx.gitClient.createBranchWithChange(change)
        if (ctx.config.pushToRemote) {
          ctx.gitClient.pushBranchToOrigin(branch)
          ctx.gitHostClient.openNewPR(branch)
        }
      }
    }
  }
}
