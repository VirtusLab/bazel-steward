package org.virtuslab.bazelsteward.app

import org.virtuslab.bazelsteward.config.repo.UpdateRulesConfig
import org.virtuslab.bazelsteward.core.DependencyKind
import org.virtuslab.bazelsteward.core.common.UpdateRules
import org.virtuslab.bazelsteward.core.library.Library

class UpdateRulesProvider(
  configs: List<UpdateRulesConfig>,
  dependencyKinds: List<DependencyKind<*>>,
) {

  companion object {
    private val defaultUpdateRules = UpdateRules()
  }

  private val applier = DependencyFilterApplier(configs, dependencyKinds)

  fun resolveForLibrary(library: Library): UpdateRules {
    val filter = applier.forLibrary(library)
    val versioningForDependency = filter.findNotNull { it.versioning }
    val bumpingForDependency = filter.findNotNull { it.bumping }
    val pinForDependency = filter.findNotNull { it.pin }
    return UpdateRules(
      versioningForDependency?.versioning ?: defaultUpdateRules.versioningSchema,
      bumpingForDependency?.bumping ?: defaultUpdateRules.bumpingStrategy,
      pinForDependency?.pin ?: defaultUpdateRules.pinningStrategy,
    )
  }
}
