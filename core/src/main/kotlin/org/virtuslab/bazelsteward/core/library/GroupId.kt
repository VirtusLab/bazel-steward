package org.virtuslab.bazelsteward.core.library

class GroupId(override val name: String) : LibraryId() {
  override fun associatedStrings(): List<List<String>> = listOf(listOf(name))
}
