package org.virtuslab.bazelsteward.core

import arrow.core.Option

fun interface Environment {
  fun get(name: String): Option<String>

  companion object {
    val system = Environment { name -> Option.fromNullable(System.getenv(name)) }
  }
}