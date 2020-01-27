/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import java.lang.reflect.Method

// https://www.yourkit.com/docs/java/api/index.html
@Suppress("unused")
object YourKit {
    private val klass = Class.forName("com.yourkit.api.Controller")
    private val controller = klass.newInstance()

    private object Methods {
        val captureMemorySnapshot: Method = klass.getMethod("captureMemorySnapshot")
        val startTracing: Method = klass.getMethod("startTracing", String::class.java)
        val stopCpuProfiling: Method = klass.getMethod("stopCpuProfiling")
        val capturePerformanceSnapshot: Method = klass.getMethod("capturePerformanceSnapshot")
    }

    fun captureMemorySnapshot(): String =
        Methods.captureMemorySnapshot(controller) as String

    fun capturePerformanceSnapshot(): String =
        Methods.capturePerformanceSnapshot(controller) as String

    fun startTracing(settings: String?) {
        Methods.startTracing(controller, settings)
    }

    fun stopCpuProfiling() {
        Methods.stopCpuProfiling(controller)
    }
}
