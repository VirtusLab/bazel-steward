package org.virtuslab.bazelsteward.core

data class GitBranch(val name: String) {
  override fun toString(): String = name
}