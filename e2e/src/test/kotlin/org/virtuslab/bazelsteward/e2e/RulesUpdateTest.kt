package org.virtuslab.bazelsteward.e2e

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

open class RulesUpdateTest(
  private val project: String,
  private val expectedBranches: List<String>
) : E2EBase() {

  @Test
  fun `Project with specific rules`(
    @TempDir tempDir: Path
  ) {
    runBazelStewardWith(tempDir, project) {
      it.withRulesOnly()
    }

    checkBranchesWithVersions(tempDir, project, expectedBranches)
  }

//  private val appleRulesPair = "rules_apple" to "2.1.0"
//  private val ccRulesPair = "rules_cc" to "0.0.6"
//  private val closureRulesPair = "rules_closure" to "0.12.0"
//  private val dockerRulesPair = "rules_docker" to "v0.25.0"
//  private val dotnetRulesPair = "rules_dotnet" to "v0.8.7" // very new
//  private val foreignCCRulesPair = "rules_foreign_cc" to "0.9.0" // rare updates
//  private val goRulesPair = "rules_go" to "v0.38.1"
//  private val javaRulesPair = "rules_java" to "5.4.1"
//  private val jvmExternalRulesPair = "rules_jvm_external" to "4.5"
//  private val kotlinRulesPair = "rules_kotlin" to "v1.7.1"
//  private val k8sRulesPair = "rules_k8s" to "v0.7" // very rare updates
//  private val protoRulesPair = "rules_proto" to "5.3.0-21.7"
//  private val pythonRulesPair = "rules_python" to "0.16.2"
//  private val scalaRulesPair = "io_bazel_rules_scala" to "v5.0.0"
//  private val skylibRulesPair = "bazel-skylib" to "1.3.0"
//
//  private fun argumentsForSpecificRules(): List<Arguments> = listOf(
////    Arguments.of(
////      "rules/trivial/all",
////      expectedBranches(
////        appleRulesPair,
////        ccRulesPair,
//////        closureRulesPair,
////        dockerRulesPair,
////        dotnetRulesPair,
////        foreignCCRulesPair,
////        goRulesPair,
////        javaRulesPair,
////        jvmExternalRulesPair,
////        kotlinRulesPair,
////        k8sRulesPair,
//////        protoRulesPair,
////        pythonRulesPair,
//////        scalaRulesPair,
////        skylibRulesPair,
////      ),
////    ),
////    Arguments.of(
////      "rules/trivial/rules_apple",
////      expectedBranches(
////        appleRulesPair,
////        jvmExternalRulesPair
////      )
//////    ),
//    Arguments.of(
//      "rules/trivial/rules_cc",
//      expectedBranches(
//        ccRulesPair,
//        jvmExternalRulesPair
//      )
////    ),
////    Arguments.of(
////      "rules/trivial/rules_closure",
////      expectedBranches(
////        closureRulesPair,
////        jvmExternalRulesPair
////      )
////    ),
////    Arguments.of(
////      "rules/trivial/rules_docker",
////      expectedBranches(
////        dockerRulesPair,
////        jvmExternalRulesPair
////      )
////    ),
////    Arguments.of(
////      "rules/trivial/rules_dotnet",
////      expectedBranches(
////        dotnetRulesPair,
////        jvmExternalRulesPair
////      )
////    ),
////    Arguments.of(
////      "rules/trivial/rules_foreign_cc",
////      expectedBranches(
////        foreignCCRulesPair,
////        jvmExternalRulesPair
////      )
////    ),
////    Arguments.of(
////      "rules/trivial/rules_go",
////      expectedBranches(
////        goRulesPair,
////        jvmExternalRulesPair
////      )
////    ),
////    Arguments.of(
////      "rules/trivial/rules_java",
////      expectedBranches(
////        javaRulesPair,
////        jvmExternalRulesPair
////      )
////    ),
////    Arguments.of(
////      "rules/trivial/rules_jvm_external",
////      expectedBranches(
////        jvmExternalRulesPair
////      )
////    ),
////    Arguments.of(
////      "rules/trivial/rules_k8s",
////      expectedBranches(
////        k8sRulesPair,
////        jvmExternalRulesPair
////      )
////    ),
////    Arguments.of(
////      "rules/trivial/rules_kotlin",
////      expectedBranches(
////        kotlinRulesPair,
////        jvmExternalRulesPair
////      )
////    ),
//    Arguments.of(
//      "rules/trivial/rules_proto",
//      expectedBranches(
//        protoRulesPair,
//        jvmExternalRulesPair
//      )
//    ),
////    Arguments.of(
////      "rules/trivial/rules_python",
////      expectedBranches(
////        pythonRulesPair,
////        jvmExternalRulesPair
////      )
////    ),
////    Arguments.of(
////      "rules/trivial/rules_scala",
////      expectedBranches(
////        scalaRulesPair,
////        skylibRulesPair,
////        jvmExternalRulesPair
////      )
////    ),
////    Arguments.of(
////      "rules/trivial/rules_skylib",
////      expectedBranches(
////        scalaRulesPair,
////        skylibRulesPair,
////        jvmExternalRulesPair
////      )
////    ),
//  )
}
