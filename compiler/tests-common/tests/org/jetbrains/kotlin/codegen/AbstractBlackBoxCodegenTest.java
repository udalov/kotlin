/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import com.intellij.openapi.util.io.FileUtil;
import kotlin.Pair;
import kotlin.collections.CollectionsKt;
import kotlin.collections.MapsKt;
import kotlin.io.FilesKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.TestsRuntimeError;
import org.jetbrains.kotlin.backend.common.CodegenUtil;
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.utils.DFS;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jetbrains.kotlin.codegen.TestUtilsKt.clearReflectionCache;
import static org.jetbrains.kotlin.test.KotlinTestUtils.assertEqualsToFile;
import static org.jetbrains.kotlin.test.clientserver.TestProcessServerKt.getBoxMethodOrNull;
import static org.jetbrains.kotlin.test.clientserver.TestProcessServerKt.getGeneratedClass;

public abstract class AbstractBlackBoxCodegenTest extends CodegenTestCase {
    @Override
    protected void doMultiFileTest(@NotNull File wholeFile, @NotNull List<TestFile> files) throws Exception {
        boolean isIgnored = InTextDirectivesUtils.isIgnoredTarget(getBackend(), wholeFile);

        List<TestModule> distinctModules = CollectionsKt.distinct(CollectionsKt.map(files, file -> file.module));
        Map<String, TestModule> moduleByName = MapsKt.toMap(CollectionsKt.map(distinctModules, module -> new Pair<>(module.name, module)));

        if (moduleByName.size() == 1) {
            doSingleModuleTest(wholeFile, files, isIgnored);
        }
        else {
            doMultiModuleTest(files, isIgnored, moduleByName);
            assert !InTextDirectivesUtils.isDirectiveDefined(FileUtil.loadFile(wholeFile), "CHECK_BYTECODE_LISTING") :
                    "Bytecode listing is not yet supported in multi-module black box codegen tests";
        }
    }

    private void doSingleModuleTest(@NotNull File wholeFile, @NotNull List<TestFile> files, boolean isIgnored) throws Exception {
        compile(files, !isIgnored);

        try {
            blackBox(!isIgnored);
        }
        catch (Throwable t) {
            if (!isIgnored) {
                try {
                    // To create .txt file in case of failure
                    doBytecodeListingTest(wholeFile);
                }
                catch (Throwable ignored) {
                }
            }

            throw new TestsRuntimeError(t);
        }

        doBytecodeListingTest(wholeFile);
    }

    private void doMultiModuleTest(@NotNull List<TestFile> files, boolean isIgnored, @NotNull Map<String, TestModule> moduleByName) throws Exception {
        List<TestModule> orderedModules = DFS.topologicalOrder(
                moduleByName.values(),
                module -> CollectionsKt.map(module.dependencies, moduleByName::get)
        );

        Map<TestModule, File> moduleOutput = new HashMap<>();

        for (TestModule module : CollectionsKt.asReversed(orderedModules)) {
            List<TestFile> moduleFiles = CollectionsKt.filter(files, file -> file.module.equals(module));
            assert !moduleFiles.isEmpty() : "No files in module " + module.name;

            File javaSourceDir = writeJavaFiles(files);
            File outputDirectory = KotlinTestUtils.tmpDir(toString());
            moduleOutput.put(module, outputDirectory);

            List<File> dependencies = CollectionsKt.map(module.dependencies, m -> moduleOutput.get(moduleByName.get(m)));

            compile(moduleFiles, dependencies, javaSourceDir, outputDirectory, !isIgnored);
        }

        try {
            // We're assuming that the 'box' method will be found in the last module.
            blackBox(!isIgnored);
        }
        catch (Throwable t) {
            throw new TestsRuntimeError(t);
        }
    }

    private void doBytecodeListingTest(@NotNull File wholeFile) throws Exception {
        if (!InTextDirectivesUtils.isDirectiveDefined(FileUtil.loadFile(wholeFile), "CHECK_BYTECODE_LISTING")) return;

        String suffix =
                (coroutinesPackage.contains("experimental") || coroutinesPackage.isEmpty())
                && InTextDirectivesUtils.isDirectiveDefined(FileUtil.loadFile(wholeFile), "COMMON_COROUTINES_TEST")
                ? "_1_2" : "";
        File expectedFile = new File(wholeFile.getParent(), FilesKt.getNameWithoutExtension(wholeFile) + suffix + ".txt");

        String text =
                BytecodeListingTextCollectingVisitor.Companion.getText(
                        classFileFactory,
                        new BytecodeListingTextCollectingVisitor.Filter() {
                            @Override
                            public boolean shouldWriteClass(int access, @NotNull String name) {
                                return !name.startsWith("helpers/");
                            }

                            @Override
                            public boolean shouldWriteMethod(int access, @NotNull String name, @NotNull String desc) {
                                return true;
                            }

                            @Override
                            public boolean shouldWriteField(int access, @NotNull String name, @NotNull String desc) {
                                return true;
                            }

                            @Override
                            public boolean shouldWriteInnerClass(@NotNull String name) {
                                return true;
                            }
                        }
                );

        assertEqualsToFile(expectedFile, text, s -> s.replace("COROUTINES_PACKAGE", coroutinesPackage));
    }

    protected void blackBox(boolean reportProblems) {
        // If there are many files, the first 'box(): String' function will be executed.
        GeneratedClassLoader generatedClassLoader = generateAndCreateClassLoader(reportProblems);
        for (KtFile firstFile : myFiles.getPsiFiles()) {
            String className = getFacadeFqName(firstFile);
            if (className == null) continue;
            Class<?> aClass = getGeneratedClass(generatedClassLoader, className);
            try {
                Method method = getBoxMethodOrNull(aClass);
                if (method != null) {
                    callBoxMethodAndCheckResult(generatedClassLoader, aClass, method);
                    return;
                }
            }
            catch (Throwable e) {
                if (reportProblems) {
                    System.out.println(generateToText());
                }
                throw ExceptionUtilsKt.rethrow(e);
            }
            finally {
                clearReflectionCache(generatedClassLoader);
            }
        }
        fail("Can't find box method!");
    }

    @Nullable
    private static String getFacadeFqName(@NotNull KtFile file) {
        return CodegenUtil.getMemberDeclarationsToGenerate(file).isEmpty()
               ? null
               : JvmFileClassUtil.getFileClassInfoNoResolve(file).getFacadeClassFqName().asString();
    }

    protected TargetBackend getBackend() {
        return TargetBackend.JVM;
    }
}
