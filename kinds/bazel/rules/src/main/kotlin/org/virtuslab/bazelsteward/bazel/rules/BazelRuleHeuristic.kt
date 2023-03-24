package org.virtuslab.bazelsteward.bazel.rules

import org.virtuslab.bazelsteward.core.common.FileChange
import org.virtuslab.bazelsteward.core.common.TextFile
import org.virtuslab.bazelsteward.core.common.UpdateSuggestion
import org.virtuslab.bazelsteward.core.replacement.LibraryUpdate
import org.virtuslab.bazelsteward.core.replacement.VersionReplacementHeuristic

object BazelRuleHeuristic : VersionReplacementHeuristic {
  override val name: String = "bazel-rule-default"

  override fun apply(files: List<TextFile>, updateSuggestion: UpdateSuggestion): LibraryUpdate? {
    if (updateSuggestion.currentLibrary is RuleLibrary && updateSuggestion.suggestedVersion is RuleVersion) {
      val ruleLibrary = updateSuggestion.currentLibrary as RuleLibrary
      val currentUrl = ruleLibrary.id.downloadUrl
      val currentVersion = updateSuggestion.currentLibrary.version.value
      val currentSha = ruleLibrary.version.sha256

      val changes = with(updateSuggestion.suggestedVersion as RuleVersion) {
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
