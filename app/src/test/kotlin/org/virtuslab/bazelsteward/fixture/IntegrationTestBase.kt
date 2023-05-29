package org.virtuslab.bazelsteward.fixture

import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.GitPlatform.PrStatus
import java.nio.file.Path

open class IntegrationTestBase {
  protected val master = "master"

  protected fun prepareWorkspace(tempDir: Path, resourcePath: String, extraDirs: List<String> = emptyList()): Path {
    return prepareLocalWorkspace(tempDir, resourcePath, extraDirs)
      .also { localWorkspace -> prepareRemoteWorkspace(tempDir, resourcePath, localWorkspace, master) }
  }

  protected fun mockGitHostClientWithBranches(map: Map<GitBranch, PrStatus>): MockGitPlatform {
    return object : MockGitPlatform() {
      override fun checkPrStatus(branch: GitBranch): PrStatus {
        return map.getOrDefault(branch, PrStatus.NONE)
      }
    }
  }
}
