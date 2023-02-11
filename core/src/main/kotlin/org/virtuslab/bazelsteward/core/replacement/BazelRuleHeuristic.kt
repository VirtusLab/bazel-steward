package org.virtuslab.bazelsteward.core.replacement

import org.virtuslab.bazelsteward.core.common.BazelFileSearch
import org.virtuslab.bazelsteward.core.common.FileChange
import org.virtuslab.bazelsteward.core.common.FileChangeSuggestion
import org.virtuslab.bazelsteward.core.common.UpdateLogic
import org.virtuslab.bazelsteward.core.library.LibraryId
import org.virtuslab.bazelsteward.core.library.Version
import org.virtuslab.bazelsteward.core.rules.RuleLibrary
import org.virtuslab.bazelsteward.core.rules.RuleUpdateSearch
import org.virtuslab.bazelsteward.core.rules.RuleVersion

object BazelRuleHeuristic : Heuristic {
  override val name: String = "bazel-rule-default"

  override fun <Lib : LibraryId, V : Version> apply(
    files: List<BazelFileSearch.BazelFile>,
    updateSuggestion: UpdateLogic.UpdateSuggestion<Lib, V>
  ): LibraryUpdate<Lib, V>? {
    if (updateSuggestion.currentLibrary is RuleLibrary && updateSuggestion.suggestedVersion is RuleVersion) {
      val currentUrl = updateSuggestion.currentLibrary.id.downloadUrl
      val currentVersion = updateSuggestion.currentLibrary.version.value
      val currentSha = updateSuggestion.currentLibrary.id.sha256

      val changes = with(updateSuggestion.suggestedVersion) {
        listOf(currentUrl, currentVersion, currentSha).zip(listOf(url, value, sha256)).flatMap { (current, suggested) ->
          val regex = """(${Regex.escape(current)})""".toRegex()
          files
            .map { regex.findAll(it.content) to it.path }
            .map { result ->
              result.first.singleOrNull()?.groups?.first()?.range?.let {
                FileChange(
                  result.second,
                  it.first,
                  it.last - it.first + 1,
                  suggested,
                )
              }
            }
        }
      }.filterNotNull()
      return LibraryUpdate(updateSuggestion, changes)
    } else {
      return null
    }
  }
}
