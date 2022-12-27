package org.virtuslab.bazelsteward.core

import java.nio.file.Path

data class Config(val path: Path, val pushToRemote: Boolean, val baseBranch: String)
