package org.virtuslab.bazelsteward.config.repo

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import org.virtuslab.bazelsteward.core.common.PinningStrategy
import org.virtuslab.bazelsteward.core.library.BumpingStrategy
import org.virtuslab.bazelsteward.core.library.VersioningSchema

data class RepoConfig(
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  val updateRules: List<UpdateRulesConfig> = emptyList()
)

data class UpdateRulesConfig(
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  override val kinds: List<String> = emptyList(),
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  override val dependencies: List<DependencyNameFilter> = emptyList(),
  val pin: PinningStrategy? = null,
  val versioning: VersioningSchema? = null,
  val bumping: BumpingStrategy? = null,
) : DependencyFilter
