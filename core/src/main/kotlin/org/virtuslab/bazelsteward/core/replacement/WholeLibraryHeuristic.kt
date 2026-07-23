package org.virtuslab.bazelsteward.core.replacement

import org.virtuslab.bazelsteward.core.common.FileChange
import org.virtuslab.bazelsteward.core.common.TextFile
import org.virtuslab.bazelsteward.core.common.UpdateSuggestion

object WholeLibraryHeuristic : VersionReplacementHeuristic {
  override val name: String = "whole-library"

  override fun apply(files: List<TextFile>, updateSuggestion: UpdateSuggestion): LibraryUpdate? {
    val markers = updateSuggestion.currentLibrary.id.associatedStrings()
    val currentVersion = updateSuggestion.currentLibrary.version.value
    val regexes = markers.map { marker ->
      (marker + currentVersion).map { """(${Regex.escape(it)})""" }.reduce { acc, s -> "$acc.*$s" }.toRegex()
    }
    val matches = regexes.firstNotNullOfOrNull { regex ->
      files.firstNotNullOfOrNull { textFile ->
        regex.findAll(textFile.content)
          .map { MatchedText(it, textFile.path) }
          .toList()
          .takeIf { it.isNotEmpty() }
      }
    } ?: return null

    // Artifacts that differ only by a classifier (e.g. "g:a:1.0" and
    // "g:a:1.0:linux-x64") are reported identically by rules_jvm_external, so
    // they collapse into a single update yet occur on several lines. Update
    // every occurrence whose match is as tight as the tightest one. Looser
    // matches are spurious spillover from a prefix collision (e.g. the marker
    // for "junit-jupiter" also matching inside "junit-jupiter-engine").
    val minLength = matches.minOf { it.matchedText.length }
    val fileChanges = matches
      .filter { it.matchedText.length == minLength }
      .mapNotNull { match ->
        match.offsetLastMatchGroup?.let { versionOffset ->
          FileChange(
            match.origin,
            versionOffset,
            updateSuggestion.currentLibrary.version.value.length,
            updateSuggestion.suggestedVersion.value,
          )
        }
      }
      .ifEmpty { return null }

    return LibraryUpdate(updateSuggestion, fileChanges)
  }
}
