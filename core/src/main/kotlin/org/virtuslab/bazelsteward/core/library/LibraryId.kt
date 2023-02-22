package org.virtuslab.bazelsteward.core.library

abstract class LibraryId {
  abstract fun associatedStrings(): List<List<String>>
  abstract val name: String
  final override fun toString(): String = name
}
