package org.virtuslab.bazelsteward.core.library

interface Library<out Id : LibraryId> {
  val id: Id
  val version: Version
}
