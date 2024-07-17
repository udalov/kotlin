/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

object CurrentTestDataFilePath {
    private val value = ThreadLocal<String>()

    fun get(): String? = value.get()

    fun set(new: String?) {
        if (new == null) value.remove()
        else value.set(new)
    }
}
