package org.virtuslab.bazelsteward.bazel.rules

import org.virtuslab.bazelsteward.core.common.FileChange
import org.virtuslab.bazelsteward.core.common.TextFile
import org.virtuslab.bazelsteward.core.common.UpdateSuggestion
import org.virtuslab.bazelsteward.core.replacement.LibraryUpdate
import org.virtuslab.bazelsteward.core.replacement.VersionReplacementHeuristic

object BazelRuleHeuristic : VersionReplacementHeuristic {
  override val name: String = "bazel-rule-default"

  private class ReplaceRequest(
    current: String,
    val suggested: String,
    forbiddenSurrounding: String? = null,
  ) {
    val regex = if (forbiddenSurrounding != null) {
      """(?<!$forbiddenSurrounding)(${Regex.escape(current)})(?!$forbiddenSurrounding)""".toRegex()
    } else {
      """(${Regex.escape(current)})""".toRegex()
    }
  }

  override fun apply(files: List<TextFile>, updateSuggestion: UpdateSuggestion): LibraryUpdate? {
    if (updateSuggestion.currentLibrary is RuleLibrary && updateSuggestion.suggestedVersion is RuleVersion) {
      val ruleLibrary = updateSuggestion.currentLibrary as RuleLibrary
      val currentUrl = ruleLibrary.id.downloadUrl
      val currentVersion = updateSuggestion.currentLibrary.version.value
      val currentSha = ruleLibrary.version.sha256

      val suggestedRuleVersion = updateSuggestion.suggestedVersion as RuleVersion
      val suggestedUrl = suggestedRuleVersion.url
      val suggestedVersion = suggestedRuleVersion.value
      val suggestedSha = suggestedRuleVersion.sha256

      val replaceRequests = listOf(
        ReplaceRequest(currentUrl, suggestedUrl),
        ReplaceRequest(currentSha, suggestedSha, "[a-z0-9]"),
        ReplaceRequest(currentVersion, suggestedVersion, "[0-9]"),
      )

      val groupedChanges = files.map { file ->
        replaceRequests.map { request ->
          val matches = request.regex.findAll(file.content)
          matches.mapNotNull { match ->
            match.groups.first()?.range?.let { matchRange ->
              FileChange(
                file.path,
                matchRange.first,
                matchRange.last - matchRange.first + 1,
                request.suggested,
              )
            }
          }.toList()
        }.filter { it.isNotEmpty() }
      }.filter { it.isNotEmpty() }.sortedByDescending { it.size }

      if (groupedChanges.isEmpty()) {
        return null
      }

      val selectedChanges = groupedChanges.takeWhile { it.size >= 2 }.takeUnless { it.isEmpty() } ?: groupedChanges
      val changes = selectedChanges.flatten().flatten()
      return LibraryUpdate(updateSuggestion, changes)
    } else {
      return null
    }
  }
}
