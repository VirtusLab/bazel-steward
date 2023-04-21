package org.virtuslab.bazelsteward.bazel.rules

import org.virtuslab.bazelsteward.core.library.Version
import java.time.Instant

sealed class RuleVersion(val tag: String, override val date: Instant?) : Version() {
  abstract val url: String
  override val value: String = tag
  abstract val sha256: String

  data class Details(val sha256: String, val url: String)

  companion object {
    fun create(url: String, sha256: String, tag: String, date: Instant): RuleVersion {
      return PlainRuleVersion(url, sha256, tag, date)
    }

    fun create(tag: String, date: Instant, supplier: () -> Details): RuleVersion {
      return LazyRuleVersion(supplier, tag, date)
    }

    private class LazyRuleVersion(
      supplier: () -> Details,
      tag: String,
      date: Instant?,
    ) : RuleVersion(tag, date) {
      private val details: Details by lazy { supplier() }
      override val url: String by lazy { details.url }
      override val sha256: String by lazy { details.sha256 }
    }

    private class PlainRuleVersion(
      override val url: String,
      override val sha256: String,
      tag: String,
      date: Instant?,
    ) : RuleVersion(tag, date)
  }
}
