package org.virtuslab.bazelsteward.app

import org.virtuslab.bazelsteward.core.GitPlatform.PrStatus
import org.virtuslab.bazelsteward.core.GitPlatform.PrStatus.NONE
import org.virtuslab.bazelsteward.core.GitPlatform.PrStatus.OPEN_MERGEABLE
import org.virtuslab.bazelsteward.core.GitPlatform.PrStatus.OPEN_MODIFIED
import org.virtuslab.bazelsteward.core.GitPlatform.PrStatus.OPEN_NOT_MERGEABLE

class PullRequestsLimits(
  private var openPrs: Int,
  private val maxOpenPrs: Int?,
  private val maxUpdates: Int?,
  private val updateAllPullRequests: Boolean,
) {
  sealed interface Result {
    infix fun and(other: Result): Result =
      when (this) {
        is Ok -> other
        is Blocked -> if (other is Blocked) Blocked("$reason and ${other.reason}") else this
      }

    object Ok : Result
    data class Blocked(val reason: String) : Result
    companion object {
      fun check(condition: Boolean, reason: () -> String): Result =
        if (condition) Ok else Blocked(reason())
    }
  }

  private var updatesLeft: Int = maxUpdates ?: Int.MAX_VALUE

  fun registerUpdate() {
    if (maxUpdates != null) {
      updatesLeft--
    }
  }

  fun registerOpenPr() {
    if (maxOpenPrs != null) {
      openPrs++
    }
  }

  fun registerClosedPrs(count: Int) {
    if (maxOpenPrs != null) {
      openPrs -= count
    }
  }

  fun canCreateOrUpdate(prStatus: PrStatus): Result {
    fun updatesLimit() = Result.check(maxUpdates == null || updatesLeft > 0) {
      "max updates limit reached ($maxUpdates)"
    }
    fun openPrsLimit() = Result.check(maxOpenPrs == null || openPrs < maxOpenPrs) {
      "max open PRs limit reached ($maxOpenPrs)"
    }
    fun updateModifiedOrMergablePr() = Result.check(updateAllPullRequests) {
      "PR is ${if (prStatus == OPEN_MERGEABLE) "mergeable" else "modified by user"} and --update-all-prs is false"
    }

    return when (prStatus) {
      NONE -> openPrsLimit() and updatesLimit()
      OPEN_NOT_MERGEABLE -> updatesLimit()
      OPEN_MERGEABLE, OPEN_MODIFIED -> updatesLimit() and updateModifiedOrMergablePr()
      else -> Result.Blocked("PR is ${prStatus.name}")
    }
  }
}
