package org.virtuslab.bazelsteward.e2e

import io.kotest.common.runBlocking
import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions
import org.virtuslab.bazelsteward.app.App
import org.virtuslab.bazelsteward.app.AppBuilder
import org.virtuslab.bazelsteward.app.BazelStewardGitBranch
import org.virtuslab.bazelsteward.core.Environment
import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.GitHostClient
import org.virtuslab.bazelsteward.core.common.GitClient
import org.virtuslab.bazelsteward.core.library.SemanticVersion
import org.virtuslab.bazelsteward.core.library.Version
import org.virtuslab.bazelsteward.maven.MavenCoordinates
import org.virtuslab.bazelsteward.maven.MavenData
import org.virtuslab.bazelsteward.maven.MavenDataExtractor
import org.virtuslab.bazelsteward.maven.MavenDependencyKind
import org.virtuslab.bazelsteward.maven.MavenRepository
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarFile
import kotlin.io.path.createDirectories

open class E2EBase {
  protected val heads = "refs/heads/"
  private val branchRef = "$heads${BazelStewardGitBranch.bazelPrefix}"
  private val master = "master"
  protected val masterRef = "$heads$master"

  protected fun branch(libraryId: String, version: String): String =
    "$branchRef/$libraryId/$version"

  protected fun expectedBranches(vararg libs: Pair<String, String>): List<String> {
    return libs.map { "$branchRef/${it.first}/${it.second}" } + masterRef
  }

  protected fun expectedBranchPrefixes(vararg libs: String): List<String> {
    return libs.map { "$branchRef/$it/" } + masterRef
  }

  protected fun runBazelSteward(tempDir: Path, project: String, args: List<String> = listOf("--push-to-remote")) {
    runBazelStewardWith(tempDir, project, args) { x -> x }
  }

  protected fun runBazelSteward(workspaceRoot: Path, args: List<String> = listOf("--push-to-remote")) {
    runBazelStewardWith(workspaceRoot, args) { x -> x }
  }

  protected fun runBazelStewardWith(
    tempDir: Path,
    project: String,
    args: List<String> = listOf("--push-to-remote"),
    transform: (App) -> App
  ) {
    val file = prepareWorkspace(tempDir, project)
    runBazelStewardWith(file, args, transform)
  }

  protected fun runBazelStewardWith(
    workspaceRoot: Path,
    args: List<String> = listOf("--push-to-remote"),
    transform: (App) -> App
  ) {
    val app = transform(AppBuilder.fromArgs(arrayOf(workspaceRoot.toString()) + args, Environment.system))
    runBlocking {
      app.run()
    }
  }

  protected fun prepareWorkspace(tempDir: Path, testResourcePath: String): Path {
    val localRepo = tempDir.resolve("local")
    val finalFile = localRepo.resolve(testResourcePath)
    val names = JarFile(File(javaClass.protectionDomain.codeSource.location.toURI())).use { jar ->
      val entries = jar.entries().asIterator().asSequence()
      entries.filterNot { it.isDirectory }.map { it.name }.filter { it.startsWith(testResourcePath) }.toList()
    }
    names.forEach { name ->
      FileUtils.copyURLToFile(
        javaClass.classLoader.getResource(name),
        localRepo.resolve(name.removeSuffix(".bzlignore")).toFile()
      )
    }

    runBlocking {
      val remoteRepo = tempDir.resolve("remote")
      remoteRepo.createDirectories()
      GitClient(remoteRepo).init(bare = true)

      val git = GitClient(finalFile)
      git.init(initialBranch = master)
      git.configureAuthor("bazel-steward@virtuslab.org", "Bazel Steward")
      git.add(Paths.get("."))
      git.commit("Maven test $testResourcePath")
      git.remoteAdd("origin", remoteRepo.toString())
      git.push(master)
    }
    return finalFile
  }

  protected fun checkBranchesWithVersions(
    tempDir: Path,
    testResourcePath: String,
    branches: List<String>,
    skipLocal: Boolean = false,
    skipRemote: Boolean = false
  ) {
    val localRepo = tempDir.resolve("local").resolve(testResourcePath)
    val remoteRepo = tempDir.resolve("remote")

    if (!skipLocal) {
      checkForBranchesWithVersions(localRepo, branches)
      checkStatusOfBranches(localRepo, branches)
    }
    if (!skipRemote)
      checkForBranchesWithVersions(remoteRepo, branches)
  }

  protected fun checkBranchesWithoutVersions(tempDir: Path, testResourcePath: String, branchesPattern: List<String>) {
    val localRepo = tempDir.resolve("local").resolve(testResourcePath)
    val remoteRepo = tempDir.resolve("remote")

    checkForBranchesWithoutVersions(localRepo, branchesPattern)
    checkForBranchesWithoutVersions(remoteRepo, branchesPattern)

    val git = GitClient(localRepo)
    val gitBranches = runBlocking { git.showRef(heads = true) }
    checkStatusOfBranches(localRepo, gitBranches)
  }

  private fun checkForBranchesWithoutVersions(tempDir: Path, requiredBranches: List<String>) {
    val git = GitClient(tempDir)
    val gitBranches = runBlocking { git.showRef(heads = true) }

    Assertions.assertThat(gitBranches).hasSameSizeAs(requiredBranches)
    Assertions.assertThat(requiredBranches.all { branch -> gitBranches.any { it.contains(branch) } }).isTrue
  }

  private fun checkForBranchesWithVersions(tempDir: Path, branches: List<String>) {
    val git = GitClient(tempDir)
    val gitBranches = runBlocking { git.showRef(heads = true) }
    Assertions.assertThat(gitBranches).containsExactlyInAnyOrderElementsOf(branches)
  }

  private fun checkStatusOfBranches(tempDir: Path, branches: List<String>) {
    val git = GitClient(tempDir)
    runBlocking {
      branches.forEach { branchRef ->
        val branch = branchRef.removePrefix(heads)
        git.checkout(branch)
        val status = git.status()
        Assertions.assertThat(status)
          .contains("Your branch is up to date with")
          .contains("nothing to commit, working tree clean")
      }
    }
  }

  protected fun App.withMavenOnly(versions: List<String>): App {
    return this.copy(
      dependencyKinds = listOf(
        MavenDependencyKind(
          MavenDataExtractor(this.appConfig),
          mockMavenRepositoryWithVersion(*versions.toTypedArray())
        )
      )
    )
  }

  protected fun App.withGitHostClient(gitHostClient: GitHostClient): App {
    return this.copy(
      gitHostClient = gitHostClient
    )
  }

  protected fun App.withPRStatus(status: GitHostClient.PrStatus): App {
    return this.withGitHostClient(mockGitHostClientWithStatus(status))
  }

  protected fun mockMavenRepositoryWithVersion(vararg versions: String): MavenRepository {
    return object : MavenRepository() {
      override suspend fun findVersions(mavenData: MavenData): Map<MavenCoordinates, List<Version>> =
        mapOf(mavenData.dependencies[0] to versions.mapNotNull { SemanticVersion.fromString(it) }.toList())
    }
  }

  protected fun mockGitHostClientWithStatus(status: GitHostClient.PrStatus): CountingGitHostClient {
    return object : CountingGitHostClient() {
      override fun checkPrStatus(branch: GitBranch) = status
    }
  }
}
