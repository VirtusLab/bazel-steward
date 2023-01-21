package org.virtuslab.bazelsteward.app

import mu.KotlinLogging
import org.virtuslab.bazelsteward.common.GitOperations

private val logger = KotlinLogging.logger {}

class App(private val ctx: Context) {
  suspend fun run() {
    ctx.gitOperations.checkoutBaseBranch()
    val definitions = ctx.bazelFileSearch.buildDefinitions
    logger.debug { definitions.map { it.first } }
    val mavenData = ctx.mavenDataExtractor.extract()
    logger.debug { mavenData.repositories }
    logger.debug { mavenData.dependencies.map { it.id.name + " " + it.version.value } }
    val availableVersions = ctx.mavenRepository.findVersions(mavenData)
    val updateSuggestions = availableVersions.mapNotNull {
      ctx.updateLogic.selectUpdate(it.key, it.value)
    }
    val changeSuggestions = ctx.fileUpdateSearch.searchBuildFiles(definitions, updateSuggestions)

    changeSuggestions.forEach { change ->
      val branch = GitOperations.Companion.fileChangeSuggestionToBranch(change)
      if (!ctx.gitHostClient.checkIfPrExists(branch)) {
        logger.info { "Creating branch ${branch.name}" }
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
