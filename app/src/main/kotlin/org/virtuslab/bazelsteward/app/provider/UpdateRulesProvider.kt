package org.virtuslab.bazelsteward.app.provider

import org.virtuslab.bazelsteward.app.DependencyFilterApplier
import org.virtuslab.bazelsteward.config.repo.UpdateRulesConfig
import org.virtuslab.bazelsteward.core.DependencyKind
import org.virtuslab.bazelsteward.core.common.UpdateRules
import org.virtuslab.bazelsteward.core.library.Library

class UpdateRulesProvider(
  configs: List<UpdateRulesConfig>,
  dependencyKinds: List<DependencyKind<*>>,
) {

  private val applier = DependencyFilterApplier(configs, dependencyKinds)

  fun resolveForLibrary(library: Library): UpdateRules {
    val filter = applier.forLibrary(library)
    val defaultUpdateRules = filter.kind?.defaultUpdateRules ?: UpdateRules()
    return UpdateRules(
      filter.findNotNullOrDefault(defaultUpdateRules.versioningSchema) { it.versioning },
      filter.findNotNullOrDefault(defaultUpdateRules.bumpingStrategy) { it.bumping },
      filter.findNotNullOrDefault(defaultUpdateRules.pinningStrategy) { it.pin },
    )
  }
}
