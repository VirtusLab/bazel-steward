package org.virtuslab.bazelsteward.e2e

import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.GitHostClient
import org.virtuslab.bazelsteward.core.library.LibraryId
import org.virtuslab.bazelsteward.core.library.Version

abstract class CountingGitHostClient : GitHostClient {
  val openNewPrCalls: MutableList<GitBranch> = ArrayList(10)
  val closeOldPrsCalls: MutableList<LibraryId> = ArrayList(10)
  override fun openNewPR(branch: GitBranch) {
    openNewPrCalls.add(branch)
  }

  override fun closePrs(library: LibraryId, filterNotVersion: Version?) {
    closeOldPrsCalls.add(library)
  }
}
