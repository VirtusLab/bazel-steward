package org.virtuslab.bazelsteward.core.library

interface LibraryId {
  fun associatedStrings(): List<String>

  val name: String
  val groupName: String?
}
