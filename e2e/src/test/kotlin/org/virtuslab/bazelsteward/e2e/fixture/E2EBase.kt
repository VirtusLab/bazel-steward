package org.virtuslab.bazelsteward.e2e.fixture

import io.kotest.common.runBlocking
import org.assertj.core.api.Assertions
import org.virtuslab.bazelsteward.app.App
import org.virtuslab.bazelsteward.app.AppBuilder
import org.virtuslab.bazelsteward.app.AppResult
import org.virtuslab.bazelsteward.bazel.rules.BazelRulesDependencyKind
import org.virtuslab.bazelsteward.bazel.rules.BazelRulesExtractor
import org.virtuslab.bazelsteward.bazel.rules.RuleLibraryId
import org.virtuslab.bazelsteward.bazel.rules.RulesResolver
import org.virtuslab.bazelsteward.bazel.version.BazelVersionDependencyKind
import org.virtuslab.bazelsteward.core.Environment
import org.virtuslab.bazelsteward.core.GitBranch
import org.virtuslab.bazelsteward.core.GitPlatform
import org.virtuslab.bazelsteward.core.GitPlatform.PrStatus
import org.virtuslab.bazelsteward.core.common.GitClient
import org.virtuslab.bazelsteward.core.library.SimpleVersion
import org.virtuslab.bazelsteward.core.library.Version
import org.virtuslab.bazelsteward.fixture.IntegrationTestBase
import org.virtuslab.bazelsteward.fixture.MockGitPlatform
import org.virtuslab.bazelsteward.maven.MavenCoordinates
import org.virtuslab.bazelsteward.maven.MavenData
import org.virtuslab.bazelsteward.maven.MavenDataExtractor
import org.virtuslab.bazelsteward.maven.MavenDependencyKind
import org.virtuslab.bazelsteward.maven.MavenLibraryId
import org.virtuslab.bazelsteward.maven.MavenRepository
import java.nio.file.Path
import kotlin.io.path.readText

open class E2EBase : IntegrationTestBase() {
  protected val heads = "refs/heads/"
  private val branchRef = "$heads\$bazel-steward"
  protected val masterRef = "$heads$master"

  protected fun branch(libraryId: String, version: String): String =
    "$branchRef/$libraryId/$version"

  protected fun branch(name: String): String =  "$branchRef/$name"

  protected fun expectedBranches(vararg libs: Pair<String, String>): List<String> {
    return libs.map { "$branchRef/${it.first}/${it.second}" } + masterRef
  }

  protected fun expectedBranchPrefixes(vararg libs: String): List<String> {
    return libs.map { "$branchRef/$it/" } + masterRef
  }

  protected fun runBazelSteward(tempDir: Path, project: String, args: List<String> = emptyList()): AppResult {
    return runBazelStewardWith(tempDir, project, args) { x -> x }
  }

  protected fun runBazelSteward(workspaceRoot: Path, args: List<String> = emptyList()): AppResult {
    return runBazelStewardWith(workspaceRoot, args) { x -> x }
  }

  protected fun runBazelStewardWith(
    tempDir: Path,
    project: String,
    args: List<String> = emptyList(),
    transform: (App) -> App,
  ): AppResult {
    val file = prepareWorkspace(tempDir, project)
    return runBazelStewardWith(file, args, transform)
  }

  protected fun runBazelStewardWith(
    workspaceRoot: Path,
    args: List<String> = emptyList(),
    transform: (App) -> App,
  ): AppResult {
    val app = transform(AppBuilder.fromArgs(arrayOf(workspaceRoot.toString()) + args, Environment.system))
    return runBlocking {
      app.run()
    }
  }

  protected fun checkBranchesWithVersions(
    tempDir: Path,
    testResourcePath: String,
    branches: List<String>,
    skipLocal: Boolean = false,
    skipRemote: Boolean = false,
  ) {
    val localRepo = tempDir.resolve("local").resolve(testResourcePath)
    val remoteRepo = tempDir.resolve("remote")

    if (!skipLocal) {
      checkForBranchesWithVersions(localRepo, branches)
      checkStatusOfBranches(localRepo, branches)
    }
    if (!skipRemote) {
      checkForBranchesWithVersions(remoteRepo, branches)
    }
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

  protected fun checkChangesInBranches(
    tempDir: Path,
    testResourcePath: String,
    originalFile: Path,
    resultFile: Path,
    branchName: String,
  ) {
    val localRepo = tempDir.resolve("local").resolve(testResourcePath)
    val git = GitClient(localRepo)
    runBlocking {
      val branches = git.showRef(heads = true)
      branches.single {
        it.endsWith(branchName)
      }.also { branch ->
        git.checkout(branch)
        Assertions.assertThat(originalFile.readText()).isEqualTo(resultFile.readText())
      }
    }
  }

  class MockMavenRepository : MavenRepository() {
    private var defaultVersions: List<Version>? = null
    private val state = mutableMapOf<MavenLibraryId, List<Version>>()

    fun withDefaultVersions(vararg versions: String): MockMavenRepository {
      defaultVersions = versions.map { SimpleVersion(it) }
      return this
    }

    fun withVersion(coordinates: String, version: String): MockMavenRepository {
      return withVersions(MavenLibraryId.fromString(coordinates), listOf(SimpleVersion(version)))
    }

    fun withVersion(coordinates: List<String>, version: String): MockMavenRepository {
      coordinates.forEach { withVersion(it, version) }
      return this
    }

    private fun withVersions(library: MavenLibraryId, versions: List<Version>): MockMavenRepository {
      if (state.containsKey(library)) {
        state[library] = state[library]!! + versions
      } else {
        state[library] = versions
      }
      return this
    }

    override suspend fun findVersions(mavenData: MavenData): Map<MavenCoordinates, List<Version>> {
      return mavenData.dependencies.associateWith { coords ->
        val configuredVersions = state[coords.id] ?: defaultVersions ?: emptyList()
        if (!configuredVersions.contains(coords.version)) {
          listOf(coords.version) + configuredVersions
        } else {
          configuredVersions
        }
      }
    }
  }

  protected fun App.withMockMaven(configure: MockMavenRepository.() -> Unit): App {
    return this.copy(
      dependencyKinds = listOf(
        MavenDependencyKind(
          MavenDataExtractor(this.workspaceRoot),
          MockMavenRepository().also(configure),
        ),
      ),
    )
  }

  protected fun App.withMockMavenVersions(vararg versions: String): App {
    return this.withMockMaven { withDefaultVersions(*versions) }
  }

  protected fun App.withMavenOnly(): App {
    return this.copy(
      dependencyKinds = this.dependencyKinds.filterIsInstance<MavenDependencyKind>(),
    )
  }

  protected fun App.withRulesOnly(): App {
    return this.copy(
      dependencyKinds = this.dependencyKinds.filterIsInstance<BazelRulesDependencyKind>(),
    )
  }

  protected fun App.withBazelVersionOnly(): App {
    return this.copy(
      dependencyKinds = this.dependencyKinds.filterIsInstance<BazelVersionDependencyKind>(),
    )
  }

  protected fun App.withGitHostClient(gitPlatform: GitPlatform, pushToRemote: Boolean = true): App {
    return this.copy(
      pullRequestManager = this.pullRequestManager.copy(
        gitPlatform,
        this.gitOperations,
        pushToRemote = pushToRemote,
      ),
    )
  }

  protected fun App.withGitHubRulesResolver(githubRulesResolver: RulesResolver): App {
    return this.copy(
      dependencyKinds = this.dependencyKinds.map { dependencyKind ->
        when (dependencyKind) {
          is BazelRulesDependencyKind -> BazelRulesDependencyKind(BazelRulesExtractor(), githubRulesResolver)
          else -> dependencyKind
        }
      },
    )
  }

  protected fun App.withPRStatus(status: PrStatus): App {
    return this.withGitHostClient(mockGitHostClientWithStatus(status))
  }

  protected fun mockGitHostClientWithStatus(status: PrStatus): MockGitPlatform {
    return object : MockGitPlatform() {
      override fun checkPrStatus(branch: GitBranch) = status
    }
  }

  class GithubRulesResolverMock(private val expectedVersion: Version) : RulesResolver {
    override fun resolveRuleVersions(ruleId: RuleLibraryId): List<Version> {
      return listOf(expectedVersion)
    }
  }
}
