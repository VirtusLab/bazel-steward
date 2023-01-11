package org.virtuslab.bazelsteward.app

import arrow.core.flattenOption
import org.virtuslab.bazelsteward.common.GitOperations

class App(private val ctx: Context) {
  suspend fun run() {
    ctx.gitOperations.checkoutBaseBranch()
    val definitions = ctx.bazelFileSearch.buildDefinitions
    val currentDependencies = ctx.mavenDependencyExtractor.extract()
    val availableVersions = ctx.mavenRepository.findVersions(currentDependencies)
    val updateSuggestions =
      availableVersions.map {
        ctx.updateLogic.selectUpdate(it.key, it.value)
      }.flattenOption()
    val changeSuggestions = ctx.fileUpdateSearch.searchBuildFiles(definitions, updateSuggestions)
    changeSuggestions.forEach { change ->
      val branch = GitOperations.Companion.fileChangeSuggestionToBranch(change)
      if (!ctx.gitHostClient.checkIfPrExists(branch)) {
        ctx.gitOperations.createBranchWithChange(change)
        if (ctx.config.pushToRemote) {
          ctx.gitOperations.pushBranchToOrigin(branch)
          ctx.gitHostClient.openNewPR(branch)
        }
        ctx.gitOperations.checkoutBaseBranch()
      }
    }
  }
}
