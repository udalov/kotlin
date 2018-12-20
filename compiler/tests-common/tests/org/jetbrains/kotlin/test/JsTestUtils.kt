/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.cli.js.config.JsLibraryRoot
import org.jetbrains.kotlin.utils.PathUtil

@JvmField
val JS_STDLIB = JsLibraryRoot(PathUtil.kotlinPathsForDistDirectory.jsStdLibJarPath)

@JvmField
val JS_KOTLIN_TEST = JsLibraryRoot(PathUtil.kotlinPathsForDistDirectory.jsKotlinTestJarPath)
