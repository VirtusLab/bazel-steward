package org.virtuslab.bazelsteward.core

fun interface Environment {
  operator fun get(name: String): String?
  fun getOrDefault(name: String, default: String): String = get(name) ?: default
  fun getOrThrow(name: String): String = get(name) ?: throw RuntimeException("$name not found in environment")

  companion object {
    val system = Environment { name -> System.getenv(name) }
  }
}
