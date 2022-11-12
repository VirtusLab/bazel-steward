package org.virtuslab.bazelsteward.core

interface Library<Id> {
    val id: Id
    val version: Version
}