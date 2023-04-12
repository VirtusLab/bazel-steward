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

      val suggestedRuleVersion = updateSuggestion.suggestedVersion as RuleVersion
      val suggestedUrl = suggestedRuleVersion.url
      val suggestedVersion = suggestedRuleVersion.value
      val suggestedChecksum = suggestedRuleVersion.sha256

      val changes =
        listOf(currentUrl, currentVersion, currentSha).zip(
          listOf(suggestedUrl, suggestedVersion, suggestedChecksum)
        ).flatMap { (current, suggested) ->
          val regex = """(${Regex.escape(current)})""".toRegex()
          files.flatMap { file ->
            val matches = regex.findAll(file.content)
            matches.map { match ->
              match.groups.first()?.range?.let { matchRange ->
                FileChange(
                  file.path,
                  matchRange.first,
                  matchRange.last - matchRange.first + 1,
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
