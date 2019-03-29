/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.synthetic.test

import org.jetbrains.kotlin.codegen.AbstractBytecodeTextTest
import org.jetbrains.kotlin.codegen.MutableCodegenTestCase
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind

abstract class AbstractAndroidBytecodeShapeTest : MutableCodegenTestCase() {
    private fun createAndroidAPIEnvironment(path: String) {
        return createEnvironmentForConfiguration(KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.ANDROID_API), path)
    }

    private fun createEnvironmentForConfiguration(configuration: CompilerConfiguration, path: String) {
        val layoutPaths = getResPaths(path)
        myEnvironment = createTestEnvironment(configuration, layoutPaths)
        addAndroidExtensionsRuntimeLibrary(myEnvironment)
    }

    override fun doTest(path: String) {
        val fileName = path + getTestName(true) + ".kt"
        createAndroidAPIEnvironment(path)
        loadFileByFullPath(fileName)
        val expected = AbstractBytecodeTextTest.readExpectedOccurrences(fileName)
        val actual = generateToText()
        AbstractBytecodeTextTest.checkGeneratedTextAgainstExpectedOccurrences(actual, expected)
    }
}
