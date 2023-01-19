package org.virtuslab.bazelsteward.app

import arrow.core.flattenOption
import mu.KotlinLogging
import org.virtuslab.bazelsteward.common.GitOperations

private val logger = KotlinLogging.logger {}
class App(private val ctx: Context) {
  suspend fun run() {
    ctx.gitOperations.checkoutBaseBranch()
    val definitions = ctx.bazelFileSearch.buildDefinitions
    logger.debug { definitions.map { it.first } }
    val mavenData = ctx.mavenDataExtractor.extract()
    val availableVersions = ctx.mavenRepository.findVersions(mavenData)
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
