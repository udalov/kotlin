/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js.config

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.FileBasedContentRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.io.File

data class JsLibraryRoot(override val file: File) : FileBasedContentRoot

val CompilerConfiguration.jsLibraries: List<File>
    get() = getList(CLIConfigurationKeys.CONTENT_ROOTS).filterIsInstance<JsLibraryRoot>().map(JsLibraryRoot::file)
