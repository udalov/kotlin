/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class MutableCodegenTestCase : CodegenTestCase() {
    // protected lateinit var myFiles: CodegenTestFiles

    protected fun loadText(text: String) {
        myFiles = CodegenTestFiles.create("a_test.kt", text, myEnvironment.project)
    }

    protected fun loadFile() {
        loadFile(prefix + "/" + getTestName(true) + ".kt")
    }

    protected fun loadFile(@TestDataFile name: String): String {
        return loadFileByFullPath(KotlinTestUtils.getTestDataPathBase() + "/codegen/" + name)
    }

    protected fun loadFileByFullPath(fullPath: String): String {
        val file = File(fullPath)
        val content = file.readText()
        assert(myFiles == null) { "Should not initialize myFiles twice" }
        myFiles = CodegenTestFiles.create(file.name, content, myEnvironment.project)
        return content
    }

    override fun loadFiles(vararg names: String): CodegenTestFiles {
        return super.loadFiles(*names).also { myFiles = it }
    }

    override fun loadMultiFiles(files: List<TestFile>): CodegenTestFiles {
        return super.loadMultiFiles(files).also { myFiles = it }
    }

    protected fun generateToText(): String {
        return generateToText(myFiles)
    }
}
