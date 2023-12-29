package org.virtuslab.bazelsteward.core.library

enum class BumpingStrategy {
  Minimal, Latest, MinorPatchMajor, LatestByDate, PatchOnly, PatchMinor, MinorPatch
}
