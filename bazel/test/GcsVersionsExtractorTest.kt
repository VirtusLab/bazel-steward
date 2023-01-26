package bazel.test

import bazel.src.GcsVersionsExtractor
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class GcsVersionsExtractorTest {
  @Test
  fun `should extract some versions`() {
    runBlocking {
      val versions = GcsVersionsExtractor().getVersionPrefixes()
      println(versions)
      assert(versions.isNotEmpty())
    }
  }
  @Test
  fun `should list versions with prefix`() {
    runBlocking {
      val versions = GcsVersionsExtractor().listDirectoriesInReleaseBucket("6.0.0/")
      println(versions)
      assert(versions.isNotEmpty())
    }
  }
}
