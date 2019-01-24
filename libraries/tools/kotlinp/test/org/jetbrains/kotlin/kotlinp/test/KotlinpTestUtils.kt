/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp.test

import com.intellij.openapi.Disposable
import junit.framework.TestCase.assertEquals
import kotlinx.metadata.Flag
import kotlinx.metadata.isLocal
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.KotlinModuleMetadata
import org.jetbrains.kotlin.checkers.setupLanguageVersionSettingsForCompilerTests
import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoot
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.codegen.GenerationUtils
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.jvm.compiler.AbstractLoadJavaTest
import org.jetbrains.kotlin.kotlinp.Kotlinp
import org.jetbrains.kotlin.kotlinp.KotlinpSettings
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinBaseTest
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File
import kotlin.test.fail

fun compileAndPrintAllFiles(
    file: File,
    disposable: Disposable,
    tmpdir: File,
    compareWithTxt: Boolean,
    readWriteAndCompare: Boolean,
    useIr: Boolean = false,
    skipNonPublicAPI: Boolean = false,
): String? {
    val main = StringBuilder()
    val afterVisitors = StringBuilder()
    val afterNodes = StringBuilder()

    val kotlinp = Kotlinp(KotlinpSettings(isVerbose = true))

    compile(file, disposable, tmpdir, useIr) { outputFile ->
        when (outputFile.extension) {
            "kotlin_module" -> {
                val moduleFile = kotlinp.readModuleFile(outputFile)!!
                val transformedWithVisitors = transformModuleFileWithReadWriteVisitors(moduleFile)
                val transformedWithNodes = transformModuleFileWithNodes(moduleFile)

                for ((sb, moduleFileToRender) in listOf(
                    main to moduleFile, afterVisitors to transformedWithVisitors, afterNodes to transformedWithNodes
                )) {
                    sb.appendFileName(outputFile.relativeTo(tmpdir))
                    sb.append(kotlinp.renderModuleFile(moduleFileToRender))
                }
            }
            "class" -> {
                val classFile = kotlinp.readClassFile(outputFile)!!
                if (!skipNonPublicAPI || isPublicAPI(classFile)) {
                    val classFile2 = transformClassFileWithReadWriteVisitors(classFile)
                    val classFile3 = transformClassFileWithNodes(classFile)

                    for ((sb, classFileToRender) in listOf(
                        main to classFile, afterVisitors to classFile2, afterNodes to classFile3
                    )) {
                        sb.appendFileName(outputFile.relativeTo(tmpdir))
                        sb.append(kotlinp.renderClassFile(classFileToRender))
                    }
                }
            }
            else -> fail("Unknown file: $outputFile")
        }
    }

    if (compareWithTxt) {
        KotlinTestUtils.assertEqualsToFile(File(file.path.replace(".kt", ".txt")), main.toString())
    }

    if (readWriteAndCompare && InTextDirectivesUtils.findStringWithPrefixes(file.readText(), "// NO_READ_WRITE_COMPARE") == null) {
        assertEquals("Metadata is different after transformation with visitors.", main.toString(), afterVisitors.toString())
        assertEquals("Metadata is different after transformation with nodes.", main.toString(), afterNodes.toString())
    }

    return main.toString()
}

private fun isPublicAPI(classFile: KotlinClassMetadata): Boolean {
    if (classFile is KotlinClassMetadata.SyntheticClass) return false
    if (classFile is KotlinClassMetadata.Class) {
        val klass = classFile.toKmClass()
        if (klass.name.isLocal) return false

        // Temporarily ignore inline classes because it's an experimental feature, and there are known differences in ABI
        // in equals/equals-impl, hashCode/hashCode-impl, toString/toString-impl.
        if (Flag.Class.IS_INLINE(klass.flags)) return false
    }

    return true
}

private fun compile(wholeFile: File, disposable: Disposable, tmpdir: File, useIr: Boolean, forEachOutputFile: (File) -> Unit) {
    val wholeContent = wholeFile.readText().replace("COROUTINES_PACKAGE", "kotlin.coroutines")
    val files = CodegenTestCase.createTestFilesFromFile(
        wholeFile, wholeContent, "kotlin.coroutines", false, if (useIr) TargetBackend.JVM_IR else TargetBackend.JVM
    )

    val javaFiles = files.filter { it.name.endsWith(".java") }
    val javaFilesDir =
        if (javaFiles.isEmpty()) null
        else File(tmpdir, "java-sources").also { dir ->
            for (javaFile in javaFiles) {
                File(dir, javaFile.name).apply { parentFile.mkdirs() }.writeText(javaFile.content)
            }
        }

    val configurationKind = KotlinBaseTest.extractConfigurationKind(files)
    val testJdkKind = KotlinBaseTest.getTestJdkKind(files)

    val configuration = KotlinTestUtils.newConfiguration(configurationKind, testJdkKind).apply {
        if (useIr) put(JVMConfigurationKeys.IR, true)
        javaFilesDir?.let(::addJavaSourceRoot)
        KotlinBaseTest.updateConfigurationByDirectivesInTestFiles(files, this)
        AbstractLoadJavaTest.updateConfigurationWithDirectives(wholeContent, this)
    }
    val environment = KotlinCoreEnvironment.createForTests(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

    setupLanguageVersionSettingsForCompilerTests(wholeContent, environment)

    val output = File(tmpdir, "output").apply { mkdirs() }
    GenerationUtils.compileFilesTo(
        files.filter { it.name.endsWith(".kt") }.map { file ->
            val content = CheckerTestUtil.parseDiagnosedRanges(file.content, ArrayList(0))
            KotlinTestUtils.createFile(file.name, content, environment.project)
        },
        environment, output
    )

    for (outputFile in output.walkTopDown().sortedBy { it.nameWithoutExtension }) {
        if (outputFile.isFile) {
            forEachOutputFile(outputFile)
        }
    }
}

private fun StringBuilder.appendFileName(file: File) {
    appendLine("// ${file.invariantSeparatorsPath}")
    appendLine("// ------------------------------------------")
}

// Reads the class file and writes it back with *Writer visitors.
// The resulting class file should be the same from the point of view of any metadata reader, including kotlinp
// (the exact bytes may differ though, because there are multiple ways to encode the same metadata)
private fun transformClassFileWithReadWriteVisitors(classFile: KotlinClassMetadata): KotlinClassMetadata =
    when (classFile) {
        is KotlinClassMetadata.Class -> KotlinClassMetadata.Class.Writer().apply(classFile::accept).write()
        is KotlinClassMetadata.FileFacade -> KotlinClassMetadata.FileFacade.Writer().apply(classFile::accept).write()
        is KotlinClassMetadata.SyntheticClass -> {
            val writer = KotlinClassMetadata.SyntheticClass.Writer()
            if (classFile.isLambda) {
                classFile.accept(writer)
            }
            writer.write()
        }
        is KotlinClassMetadata.MultiFileClassFacade -> KotlinClassMetadata.MultiFileClassFacade.Writer().write(classFile.partClassNames)
        is KotlinClassMetadata.MultiFileClassPart ->
            KotlinClassMetadata.MultiFileClassPart.Writer().apply(classFile::accept).write(classFile.facadeClassName)
        else -> classFile
    }

// Reads the class file and writes it back with KmClass/KmFunction/... elements.
private fun transformClassFileWithNodes(classFile: KotlinClassMetadata): KotlinClassMetadata =
    when (classFile) {
        is KotlinClassMetadata.Class ->
            KotlinClassMetadata.Class.Writer().apply(classFile.toKmClass()::accept).write()
        is KotlinClassMetadata.FileFacade ->
            KotlinClassMetadata.FileFacade.Writer().apply(classFile.toKmPackage()::accept).write()
        is KotlinClassMetadata.SyntheticClass ->
            KotlinClassMetadata.SyntheticClass.Writer().apply { classFile.toKmLambda()?.accept(this) }.write()
        is KotlinClassMetadata.MultiFileClassPart ->
            KotlinClassMetadata.MultiFileClassPart.Writer().apply(classFile.toKmPackage()::accept).write(classFile.facadeClassName)
        else -> classFile
    }

private fun transformModuleFileWithReadWriteVisitors(moduleFile: KotlinModuleMetadata): KotlinModuleMetadata =
    KotlinModuleMetadata.Writer().apply(moduleFile::accept).write()

private fun transformModuleFileWithNodes(moduleFile: KotlinModuleMetadata): KotlinModuleMetadata =
    KotlinModuleMetadata.Writer().apply(moduleFile.toKmModule()::accept).write()
