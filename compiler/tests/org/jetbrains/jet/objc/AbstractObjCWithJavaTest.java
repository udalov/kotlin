/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.objc;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.UsefulTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.common.output.outputUtils.OutputUtilsPackage;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.codegen.ClassBuilderFactories;
import org.jetbrains.jet.codegen.CompilationErrorHandler;
import org.jetbrains.jet.codegen.KotlinCodegenFacade;
import org.jetbrains.jet.codegen.ObjCDescriptorCodegen;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.utils.UtilsPackage;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import static org.jetbrains.jet.objc.ObjCTestUtil.*;

public abstract class AbstractObjCWithJavaTest extends UsefulTestCase {
    public static final String KOTLIN_FOUNDATION_HEADER_PATH = "objc/foundation/foundation.h";
    public static final String KOTLIN_FOUNDATION_SOURCE_PATH = "objc/foundation/foundation.kt";
    public static final String FOUNDATION_DYLIB_PATH = "/System/Library/Frameworks/Foundation.framework/Versions/Current/Foundation";

    private File tmpDir;
    private Project project;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        tmpDir = JetTestUtils.tmpDirForTest(this);
        JetCoreEnvironment environment = createEnvironment(getTestRootDisposable());
        project = environment.getProject();
    }

    @Override
    protected void tearDown() throws Exception {
        tmpDir = null;
        project = null;

        super.tearDown();
    }

    protected void doTest(@NotNull String kotlinSource) {
        assert kotlinSource.endsWith(".kt") : kotlinSource;
        String fileNameCommon = kotlinSource.substring(0, kotlinSource.length() - ".kt".length());
        String header = fileNameCommon + ".h";
        String implementation = fileNameCommon + ".m";

        // If .m exists, it's compiled into a .dylib and the result is assumed to be dynamically linked to Foundation.
        // Otherwise, we take Foundation dylib from the standard system path
        File dylib;
        if (new File(implementation).exists()) {
            dylib = new File(tmpDir, "libKotlinObjCTest.dylib");
            compileObjectiveC(implementation, dylib);
        }
        else {
            dylib = new File(FOUNDATION_DYLIB_PATH);
        }

        String actual = runTestGetOutput(kotlinSource, header, dylib);
        assertEquals("OK", actual);
    }

    @NotNull
    protected String runTestGetOutput(@NotNull String kotlinSource, @NotNull String header, @NotNull File dylib) {
        File headerFile = combineHeaders(KOTLIN_FOUNDATION_HEADER_PATH, header);

        List<JetFile> files = Arrays.asList(
                createJetFile(kotlinSource),
                createJetFile(KOTLIN_FOUNDATION_SOURCE_PATH)
        );
        AnalyzeExhaust analyzeExhaust = analyze(project, files, headerFile);

        PackageViewDescriptor objcPackage = extractObjCPackageFromAnalyzeExhaust(analyzeExhaust);

        ObjCDescriptorCodegen codegen = new ObjCDescriptorCodegen();
        codegen.generate(objcPackage, tmpDir, dylib);

        generate(files, analyzeExhaust, codegen.getBindingContext());

        return runCompiledKotlinClass();
    }

    // Creates a single header file containing "#import " of all of the given header files
    private static File combineHeaders(@NotNull String... headers) {
        try {
            File file = FileUtil.createTempFile("objc-java-header", ".h");
            PrintWriter out = new PrintWriter(file);
            try {
                for (String header : headers) {
                    File headerFile = new File(header);
                    if (headerFile.exists()) {
                        out.println("#import \"" + headerFile.getAbsolutePath() + "\"");
                    }
                }
            } finally {
                out.close();
            }

            return file;
        }
        catch (IOException e) {
            throw UtilsPackage.rethrow(e);
        }
    }

    @NotNull
    private String runCompiledKotlinClass() {
        String classpath = ".:" + tmpDir + ":" + getKotlinRuntimeJarFile() + ":" + getKotlinObjCRuntimeJarFile();
        String command = "java -cp " + classpath + " " + PackageClassUtils.getPackageClassFqName(new FqName("test"));
        return runProcess(command);
    }

    private static void compileObjectiveC(@NotNull String filename, @NotNull File out) {
        runProcess(String.format("clang -ObjC -dynamiclib -framework Foundation %s -o %s", filename, out));
    }

    @NotNull
    private static File getKotlinRuntimeJarFile() {
        File kotlinRuntime = new File("dist/kotlinc/lib/kotlin-runtime.jar");
        assert kotlinRuntime.exists() : "kotlin-runtime.jar should exist before this test, run dist";
        return kotlinRuntime;
    }

    private void generate(@NotNull List<JetFile> files, @NotNull AnalyzeExhaust analyzeExhaust, @NotNull BindingContext objcBinding) {
        BindingContext context = new ChainedBindingContext(analyzeExhaust.getBindingContext(), objcBinding);

        GenerationState state =
                new GenerationState(project, ClassBuilderFactories.TEST, analyzeExhaust.getModuleDescriptor(), context, files);
        KotlinCodegenFacade.compileCorrectFiles(state, CompilationErrorHandler.THROW_EXCEPTION);

        OutputUtilsPackage.writeAllTo(state.getFactory(), tmpDir);
    }

    @NotNull
    private JetFile createJetFile(@NotNull String fileName) {
        try {
            String content = FileUtil.loadFile(new File(fileName), true);
            return JetTestUtils.createFile(fileName, content, project);
        }
        catch (IOException e) {
            throw UtilsPackage.rethrow(e);
        }
    }
}
