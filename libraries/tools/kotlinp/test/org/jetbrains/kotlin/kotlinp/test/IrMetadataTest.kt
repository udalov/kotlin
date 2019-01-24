/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp.test

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TargetBackend
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class IrMetadataTest(private val file: File) {
    private class TestDisposable : Disposable {
        override fun dispose() {}
    }

    @Test
    fun doTest() {
        val tmpdir = KotlinTestUtils.tmpDirForTest(this::class.java.simpleName, file.nameWithoutExtension)

        val withoutIr = withDisposable {
            compileAndPrintAllFiles(
                file, this, tmpdir, compareWithTxt = false, readWriteAndCompare = false, useIr = false, skipNonPublicAPI = true
            )
        }
        val withIr = withDisposable {
            compileAndPrintAllFiles(
                file, this, tmpdir, compareWithTxt = false, readWriteAndCompare = false, useIr = true, skipNonPublicAPI = true
            )
        }

        if (withIr != null) {
            Assert.assertEquals(withoutIr, withIr)
        }
    }

    private inline fun withDisposable(block: Disposable.() -> String?): String? {
        val disposable = TestDisposable()
        return try {
            block(disposable)
        } finally {
            Disposer.dispose(disposable)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun computeTestDataFiles(): Collection<Array<*>> {
            // TODO: test as much sources from compiler test data as possible
            val baseDirs = listOf(
                "compiler/testData/codegen/box"
            )

            val ignoredDirs = listOf(
                "compiler/testData/codegen/box/ranges",
            )

            return mutableListOf<Array<*>>().apply {
                for (baseDir in baseDirs) {
                    for (file in File(baseDir).walkTopDown()) {
                        if (file.extension == "kt" && ignoredDirs.none { path -> path in file.path }) {
                            if (!InTextDirectivesUtils.isIgnoredTarget(TargetBackend.JVM_IR, file) &&
                                !InTextDirectivesUtils.isIgnoredTarget(TargetBackend.JVM, file) &&
                                InTextDirectivesUtils.isCompatibleTarget(TargetBackend.JVM_IR, file) &&
                                InTextDirectivesUtils.isCompatibleTarget(TargetBackend.JVM, file)
                            ) {
                                add(arrayOf(file))
                            }
                        }
                    }
                }
            }
        }
    }
}
