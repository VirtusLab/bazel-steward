package org.virtuslab.bazelsteward.app

import bazel.src.BazelUpdater
import bazel.src.BazelVersion
import bazel.src.BazelVersionFileSearch
import mu.KotlinLogging
import org.virtuslab.bazelsteward.common.GitOperations

private val logger = KotlinLogging.logger {}

class App(private val ctx: Context) {
  suspend fun run() {
    ctx.gitOperations.checkoutBaseBranch()
    val definitions = ctx.bazelFileSearch.buildDefinitions
    logger.debug { definitions.map { it.path } }
    val mavenData = ctx.mavenDataExtractor.extract()
    logger.debug { "Repositories " + mavenData.repositories.toString() }
    logger.debug { "Dependencies: " + mavenData.dependencies.map { it.id.name + " " + it.version.value }.toString() }
    val availableVersions = ctx.mavenRepository.findVersions(mavenData)
    val updateSuggestions = availableVersions.mapNotNull {
      ctx.updateLogic.selectUpdate(it.key, it.value)
    }
    logger.debug { "UpdateSuggestions: " + updateSuggestions.map { it.currentLibrary.id.name + " to " + it.suggestedVersion.value } }
    val changeSuggestions = ctx.fileUpdateSearch.searchBuildFiles(definitions, updateSuggestions)

    val bazelVersion = runCatching { BazelVersion.extractBazelVersion(ctx.config.path) }
      .onFailure { println("Can't extract Bazel version") }.getOrNull()

    val bazelChangeSuggestions = bazelVersion?.let {
      val availableBazelVersions = ctx.bazelUpdater.availableVersions(bazelVersion)
      val bazelUpdateSuggestions =
        ctx.updateLogic.selectUpdate(BazelUpdater.Companion.BazelLibrary(bazelVersion), availableBazelVersions)

      val bazelVersionFiles = BazelVersionFileSearch(ctx.config).bazelVersionFiles
      ctx.fileUpdateSearch.searchBazelVersionFiles(bazelVersionFiles, listOfNotNull(bazelUpdateSuggestions))
    }.orEmpty()

    (changeSuggestions + bazelChangeSuggestions).forEach { change ->
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
