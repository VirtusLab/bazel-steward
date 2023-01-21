package org.virtuslab.bazelsteward.app

import mu.KotlinLogging
import org.virtuslab.bazelsteward.bazel.BazelUpdater
import org.virtuslab.bazelsteward.bazel.BazelVersion
import org.virtuslab.bazelsteward.bazel.BazelVersionFileSearch
import org.virtuslab.bazelsteward.common.GitOperations
import org.virtuslab.bazelsteward.core.GitHostClient.Companion.PrStatus.CLOSED
import org.virtuslab.bazelsteward.core.GitHostClient.Companion.PrStatus.MERGED
import org.virtuslab.bazelsteward.core.GitHostClient.Companion.PrStatus.NONE
import org.virtuslab.bazelsteward.core.GitHostClient.Companion.PrStatus.OPEN_MERGEABLE
import org.virtuslab.bazelsteward.core.GitHostClient.Companion.PrStatus.OPEN_MODIFIED
import org.virtuslab.bazelsteward.core.GitHostClient.Companion.PrStatus.OPEN_NOT_MERGEABLE

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
      .onFailure { logger.error { "Can't extract Bazel version" } }.getOrNull()

    val bazelChangeSuggestions = bazelVersion?.let {
      val availableBazelVersions = ctx.bazelUpdater.availableVersions(bazelVersion)
      val bazelUpdateSuggestions =
        ctx.updateLogic.selectUpdate(BazelUpdater.Companion.BazelLibrary(bazelVersion), availableBazelVersions)

      val bazelVersionFiles = BazelVersionFileSearch(ctx.config).bazelVersionFiles
      ctx.fileUpdateSearch.searchBazelVersionFiles(bazelVersionFiles, listOfNotNull(bazelUpdateSuggestions))
    }.orEmpty()

    (changeSuggestions + bazelChangeSuggestions).forEach { change ->
      val branch = GitOperations.Companion.fileChangeSuggestionToBranch(change)
      val prStatus = ctx.gitHostClient.checkPrStatus(branch)
      when (prStatus) {
        NONE, OPEN_NOT_MERGEABLE -> {
          logger.info { "Creating branch ${branch.name}" }
          runCatching {
            ctx.gitOperations.createBranchWithChange(change)
            if (ctx.config.pushToRemote) {
              ctx.gitOperations.pushBranchToOrigin(branch, force = prStatus == OPEN_NOT_MERGEABLE)
              if (prStatus == NONE) {
                ctx.gitHostClient.openNewPR(branch)
                ctx.gitHostClient.closeOldPrs(branch)
              }
            }
          }.exceptionOrNull()?.let { logger.error("Failed at creating branch ${branch.name}", it) }
          ctx.gitOperations.checkoutBaseBranch()
        }

        CLOSED, MERGED, OPEN_MERGEABLE, OPEN_MODIFIED -> logger.info { "Skipping ${branch.name}" }
      }
    }
  }
}
