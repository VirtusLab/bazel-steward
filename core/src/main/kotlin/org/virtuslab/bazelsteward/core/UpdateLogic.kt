package org.virtuslab.bazelsteward.core

data class UpdateSuggestion<Lib>(val currentLibrary: Lib, val suggestedVersion: Version)

class UpdateLogic {
    fun <Id, Lib : Library<Id>> selectUpdates(
        currentDependencies: List<Lib>,
        availableVersions: Map<Id, List<Version>>
    ): List<UpdateSuggestion<Lib>> {
        return emptyList()
    }
}