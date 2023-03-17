package org.virtuslab.bazelsteward.bazel.rules

import org.virtuslab.bazelsteward.bazel.rules.GithubRulesResolver.Companion.getFileChecksum
import org.virtuslab.bazelsteward.core.library.Version
import java.net.URL
import java.security.MessageDigest

@Suppress("CanBeParameter")
sealed class RuleVersion(val url: String, val tag: String) : Version() {
  override val value: String = tag
  abstract val sha256: String

  companion object {
    fun create(url: String, sha256: String?, tag: String): RuleVersion {
      return if (sha256 != null) {
        RuleVersionEager(url, sha256, tag)
      } else {
        RuleVersionLazy(url, tag)
      }
    }

    private class RuleVersionEager(url: String, override val sha256: String, tag: String) : RuleVersion(url, tag)
    private class RuleVersionLazy(url: String, tag: String) : RuleVersion(url, tag) {
      override val sha256: String by lazy {
        URL(url).getFileChecksum(MessageDigest.getInstance("SHA-256"))
      }
    }
  }
}
