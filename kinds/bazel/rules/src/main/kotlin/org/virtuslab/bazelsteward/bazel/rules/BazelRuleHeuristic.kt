package org.virtuslab.bazelsteward.bazel.rules

import org.virtuslab.bazelsteward.core.common.FileChange
import org.virtuslab.bazelsteward.core.common.TextFile
import org.virtuslab.bazelsteward.core.common.UpdateSuggestion
import org.virtuslab.bazelsteward.core.replacement.LibraryUpdate
import org.virtuslab.bazelsteward.core.replacement.VersionReplacementHeuristic

object BazelRuleHeuristic : VersionReplacementHeuristic {
  override val name: String = "bazel-rule-default"

  private class ReplaceRequest(
    private val regexes: List<Regex>,
    val suggested: String,
  ) {
    constructor(current: String, suggested: String, forbiddenSurrounding: String? = null) : this(
      listOf(
        if (forbiddenSurrounding != null) {
          """(?<!$forbiddenSurrounding)(${Regex.escape(current)})(?!$forbiddenSurrounding)""".toRegex()
        } else {
          """(${Regex.escape(current)})""".toRegex()
        },
      ),
      suggested,
    )

    fun allMatches(content: String): List<MatchResult> = regexes.flatMap { it.findAll(content).toList() }
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

      fun replaceRequestForVersion(current: String, suggested: String) =
        ReplaceRequest(
          listOf(
            """(?<=")(${Regex.escape(current)})(?=")""".toRegex(),
            """(?<![0-9]|[0-9]\.)(${Regex.escape(current)})(?="|\.tar\.gz|\.tgz|\.tar|\.zip)""".toRegex(),
          ),
          suggested,
        )

      fun replaceRequestForVersionWithoutPrefix(current: String, suggested: String, prefix: String): ReplaceRequest? {
        return if (current.startsWith(prefix) && suggested.startsWith(prefix)) {
          replaceRequestForVersion(current.removePrefix(prefix), suggested.removePrefix(prefix))
        } else {
          null
        }
      }

      val replaceRequests = listOfNotNull(
        ReplaceRequest(currentUrl, suggestedUrl),
        ReplaceRequest(currentSha, suggestedSha, forbiddenSurrounding = "[a-z0-9]"),
        replaceRequestForVersion(currentVersion, suggestedVersion),
        replaceRequestForVersionWithoutPrefix(currentVersion, suggestedVersion, "v"),
      )

      val groupedChanges = files.map { file ->
        replaceRequests.map { request ->
          val matches = request.allMatches(file.content)
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
