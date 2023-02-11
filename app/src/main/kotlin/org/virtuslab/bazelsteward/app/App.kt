package org.virtuslab.bazelsteward.app

import mu.KotlinLogging
import org.virtuslab.bazelsteward.bazel.BazelUpdater
import org.virtuslab.bazelsteward.bazel.BazelVersion
import org.virtuslab.bazelsteward.bazel.BazelVersionFileSearch
import org.virtuslab.bazelsteward.core.GitHostClient.Companion.PrStatus.CLOSED
import org.virtuslab.bazelsteward.core.GitHostClient.Companion.PrStatus.MERGED
import org.virtuslab.bazelsteward.core.GitHostClient.Companion.PrStatus.NONE
import org.virtuslab.bazelsteward.core.GitHostClient.Companion.PrStatus.OPEN_MERGEABLE
import org.virtuslab.bazelsteward.core.GitHostClient.Companion.PrStatus.OPEN_MODIFIED
import org.virtuslab.bazelsteward.core.GitHostClient.Companion.PrStatus.OPEN_NOT_MERGEABLE
import org.virtuslab.bazelsteward.core.common.GitOperations
import org.virtuslab.bazelsteward.core.config.BumpingStrategy
import org.virtuslab.bazelsteward.core.config.ConfigEntry
import org.virtuslab.bazelsteward.core.library.Library
import org.virtuslab.bazelsteward.core.library.LibraryId
import org.virtuslab.bazelsteward.core.library.SimpleVersion
import org.virtuslab.bazelsteward.core.library.VersioningSchema
import org.virtuslab.bazelsteward.core.rules.RuleLibrary
import org.virtuslab.bazelsteward.core.rules.RuleUpdateSearch
import org.virtuslab.bazelsteward.maven.MavenLibraryId

private val logger = KotlinLogging.logger {}

class App(private val ctx: Context) {
  suspend fun run() {
    ctx.gitOperations.checkoutBaseBranch()
    val definitions = ctx.bazelFileSearch.buildDefinitions

    logger.debug { definitions.map { it.key.path } }
    val mavenData = ctx.mavenDataExtractor.extract()
    logger.debug { "Repositories " + mavenData.repositories.toString() }
    logger.debug { "Dependencies: " + mavenData.dependencies.map { it.id.name + " " + it.version.value }.toString() }
    val availableVersions = ctx.mavenRepository.findVersions(mavenData)
    val availableVersionsWithVersioning = availableVersions.mapKeys {
      val (versioning, bumping) = getConfigurableSetupForLibrary(it.key)
      if (versioning != it.key.versioningSchema || bumping != it.key.bumpingStrategy) {
        it.key.copy(versioningSchema = versioning, bumpingStrategy = bumping)
      } else {
        it.key
      }
    }

    val updateSuggestions = availableVersionsWithVersioning.mapNotNull {
      ctx.updateLogic.selectUpdate(it.key, it.value)
    }
    logger.debug { "UpdateSuggestions: " + updateSuggestions.map { it.currentLibrary.id.name + " to " + it.suggestedVersion.value } }
    val changeSuggestions = ctx.fileUpdateSearch.searchBuildFiles(definitions.map { it.key }, updateSuggestions)

    val bazelVersion = runCatching { BazelVersion.extractBazelVersion(ctx.config.path) }
      .onFailure { logger.error { "Can't extract Bazel version" } }.getOrNull()

    val bazelChangeSuggestions = bazelVersion?.let {
      val availableBazelVersions = ctx.bazelUpdater.availableVersions(bazelVersion)
      val bazelLibrary = BazelUpdater.BazelLibrary(bazelVersion)
      val bazelUpdateSuggestions = ctx.updateLogic.selectUpdate(bazelLibrary, availableBazelVersions)
      val bazelVersionFiles = BazelVersionFileSearch(ctx.config).bazelVersionFiles
      ctx.fileUpdateSearch.searchBazelVersionFiles(bazelVersionFiles, listOfNotNull(bazelUpdateSuggestions))
    }.orEmpty()

    val usedBazelRules = ctx.bazelRulesExtractor.extractCurrentRules(definitions)
    val latestRules = usedBazelRules.associateWith(ctx.githubRulesResolver::resolveRuleVersions)

    val ruleUpdateSuggestions = latestRules.mapNotNull { (originalRule, versionMap) ->
      ctx.updateLogic.selectUpdate(RuleLibrary(originalRule, SimpleVersion(originalRule.tag)), versionMap.values.toList())
    }
    val ruleChangeSuggestions = RuleUpdateSearch.searchBuildFiles(definitions.map { it.key }, ruleUpdateSuggestions)

    (changeSuggestions + bazelChangeSuggestions).forEach { change ->
      val branch = change.branch
      when (val prStatus = ctx.gitHostClient.checkPrStatus(branch)) {
        NONE, OPEN_NOT_MERGEABLE -> {
          logger.info { "Creating branch ${branch.name}" }
          runCatching {
            ctx.gitOperations.createBranchWithChange(change)
            if (ctx.config.pushToRemote) {
              ctx.gitOperations.pushBranchToOrigin(branch, force = prStatus == OPEN_NOT_MERGEABLE)
              if (prStatus == NONE) {
                ctx.gitHostClient.openNewPR(branch)
                ctx.gitHostClient.closePrs(change.library.id, filterNotVersion = change.library.version)
              }
            }
          }.exceptionOrNull()?.let { logger.error("Failed at creating branch ${branch.name}", it) }
          ctx.gitOperations.checkoutBaseBranch()
        }

        CLOSED, MERGED, OPEN_MERGEABLE, OPEN_MODIFIED -> logger.info { "Skipping ${branch.name}" }
      }
    }

    ruleChangeSuggestions.forEach { change ->
      val branch = GitOperations.Companion.fileChangeSuggestionToBranch(change)
      if (ctx.gitHostClient.checkPrStatus(branch) in listOf(NONE, OPEN_NOT_MERGEABLE)) {
        logger.info { "Creating branch ${branch.name}" }
        ctx.gitOperations.createBranchWithChange(change)
        if (ctx.config.pushToRemote) {
          ctx.gitOperations.pushBranchToOrigin(branch, force = true)
          ctx.gitHostClient.openNewPR(branch)
        }
        ctx.gitOperations.checkoutBaseBranch()
      }
    }
  }

  private fun getConfigEntryFromConfigs(libraryId: MavenLibraryId, configs: List<ConfigEntry>): ConfigEntry? =
    configs.firstOrNull { it.group == libraryId.group && it.artifact == libraryId.artifact }
      ?: configs.firstOrNull { it.group == libraryId.group && it.artifact == null }
      ?: configs.firstOrNull { it.group == null && it.artifact == null }

  private fun <Lib : LibraryId> getConfigurableSetupForLibrary(library: Library<Lib>): Pair<VersioningSchema, BumpingStrategy> {
    return when (val libraryId = library.id) {
      is MavenLibraryId -> {
        val versioningForDependency = getConfigEntryFromConfigs(libraryId, ctx.bazelStewardConfig.maven.configs.filter { it.versioning != null })
        val bumpingForDependency = getConfigEntryFromConfigs(libraryId, ctx.bazelStewardConfig.maven.configs.filter { it.bumping != null })
        Pair(
          versioningForDependency?.versioning ?: VersioningSchema.Loose,
          bumpingForDependency?.bumping ?: BumpingStrategy.Default
        )
      }

      else -> Pair(VersioningSchema.Loose, BumpingStrategy.Minor)
    }
  }
}
