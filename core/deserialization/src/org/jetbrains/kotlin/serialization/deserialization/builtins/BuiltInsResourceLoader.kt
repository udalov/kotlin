/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.deserialization.builtins

import java.io.InputStream
import java.net.JarURLConnection

class BuiltInsResourceLoader {
    fun loadResource(path: String): InputStream? {
        val url = this::class.java.classLoader?.getResource(path)
            ?: ClassLoader.getSystemResource(path)
            ?: return null
        val connection = url.openConnection()
        if (connection is JarURLConnection) connection.useCaches = false
        return connection.getInputStream()
    }
}
