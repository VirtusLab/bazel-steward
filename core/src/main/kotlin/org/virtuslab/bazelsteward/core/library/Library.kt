package org.virtuslab.bazelsteward.core.library

interface Library<Id : LibraryId> {
  val id: Id
  val version: Version
}
