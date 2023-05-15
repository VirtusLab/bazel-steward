package org.virtuslab.bazelsteward.app

import org.apache.commons.text.StringSubstitutor

class TemplateApplier(variables: Map<String, String>) {
  private val stringSubstitutor = StringSubstitutor(variables).also { it.isEnableUndefinedVariableException = false }

  fun apply(template: String): String {
    return stringSubstitutor.replace(template)
  }
}
