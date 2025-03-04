package org.virtuslab.bazelsteward.config.repo

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.virtuslab.bazelsteward.core.PathPattern
import org.virtuslab.bazelsteward.core.common.HookRunFor
import org.virtuslab.bazelsteward.core.common.PinningStrategy
import org.virtuslab.bazelsteward.core.library.BumpingStrategy
import org.virtuslab.bazelsteward.core.library.GroupId
import org.virtuslab.bazelsteward.core.library.VersioningSchema

data class RepoConfig(
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  val updateRules: List<UpdateRulesConfig> = emptyList(),
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  val searchPaths: List<SearchPatternConfig> = emptyList(),
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  val pullRequests: List<PullRequestsConfig> = emptyList(),
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  val postUpdateHooks: List<PostUpdateHooksConfig> = emptyList(),
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  val maven: MavenConfig? = null,
) {
  fun withFallback(fallback: RepoConfig): RepoConfig {
    return RepoConfig(
      updateRules = updateRules + fallback.updateRules,
      searchPaths = searchPaths + fallback.searchPaths,
      pullRequests = pullRequests + fallback.pullRequests,
      postUpdateHooks = postUpdateHooks + fallback.postUpdateHooks,
      maven = maven ?: fallback.maven,
    )
  }
}

data class UpdateRulesConfig(
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  @JsonDeserialize(using = ListOrItemDeserializer::class)
  override val kinds: List<String> = emptyList(),
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  @JsonDeserialize(using = ListOrItemDeserializer::class)
  override val dependencies: List<DependencyNameFilter> = emptyList(),
  val pin: PinningStrategy? = null,
  val versioning: VersioningSchema? = null,
  val bumping: BumpingStrategy? = null,
  val enabled: Boolean? = null,
) : DependencyFilter

data class SearchPatternConfig(
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  @JsonDeserialize(using = ListOrItemDeserializer::class)
  override val kinds: List<String> = emptyList(),
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  @JsonDeserialize(using = ListOrItemDeserializer::class)
  override val dependencies: List<DependencyNameFilter> = emptyList(),
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  @JsonDeserialize(using = ListOrItemDeserializer::class)
  val pathPatterns: List<PathPattern> = emptyList(),
) : DependencyFilter

data class PullRequestLimits(
  val maxOpen: Int? = null,
  val maxUpdatesPerRun: Int? = null,
)

data class PullRequestsConfig(
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  @JsonDeserialize(using = ListOrItemDeserializer::class)
  override val kinds: List<String> = emptyList(),
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  @JsonDeserialize(using = ListOrItemDeserializer::class)
  override val dependencies: List<DependencyNameFilter> = emptyList(),
  val title: String? = null,
  val body: String? = null,
  val commitMessage: String? = null,
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  @JsonDeserialize(using = ListOrItemDeserializer::class)
  val labels: List<String> = emptyList(),
  val branchPrefix: String? = null,
  val limits: PullRequestLimits? = null,
  val groupId: GroupId? = null,
) : DependencyFilter

data class PostUpdateHooksConfig(
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  @JsonDeserialize(using = ListOrItemDeserializer::class)
  override val kinds: List<String> = emptyList(),
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  @JsonDeserialize(using = ListOrItemDeserializer::class)
  override val dependencies: List<DependencyNameFilter> = emptyList(),
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  @JsonDeserialize(using = ListOrItemDeserializer::class)
  val commands: List<String> = emptyList(),
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  @JsonDeserialize(using = ListOrItemDeserializer::class)
  val filesToCommit: List<String> = emptyList(),
  val runFor: HookRunFor? = null,
  val commitMessage: String? = null,
) : DependencyFilter

data class MavenConfig(
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  val repositoryName: String = "maven",
)
