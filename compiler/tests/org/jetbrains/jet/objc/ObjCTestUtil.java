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

import com.google.common.io.CharStreams;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.analyzer.AnalysisResult;
import org.jetbrains.jet.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.objc.AnalyzerFacadeForObjC;
import org.jetbrains.jet.lang.resolve.objc.ObjCPackageFragmentProvider;
import org.jetbrains.jet.utils.UtilsPackage;

import java.io.File;
import java.io.InputStreamReader;
import java.util.List;

public class ObjCTestUtil {
    private ObjCTestUtil() {}

    @NotNull
    public static JetCoreEnvironment createEnvironment(@NotNull Disposable disposable) {
        return JetCoreEnvironment.createForTests(
                disposable,
                JetTestUtils.compilerConfigurationForTests(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, getKotlinObjCRuntimeJarFile()),
                EnvironmentConfigFiles.JVM_CONFIG_FILES
        );
    }

    @NotNull
    public static File getKotlinObjCRuntimeJarFile() {
        File kotlinObjCRuntime = new File("dist/kotlinc/lib/kotlin-objc-runtime.jar");
        assert kotlinObjCRuntime.exists() : "kotlin-objc-runtime.jar should exist before this test, run dist";
        return kotlinObjCRuntime;
    }

    @NotNull
    public static AnalysisResult analyze(@NotNull Project project, @NotNull List<JetFile> files, @NotNull File headerFile) {
        project.putUserData(ObjCPackageFragmentProvider.OBJC_PROJECT_HEADER_KEY, headerFile);

        AnalysisResult analysisResult = AnalyzerFacadeForObjC.analyzeFiles(project, files);
        analysisResult.throwIfError();
        AnalyzingUtils.throwExceptionOnErrors(analysisResult.getBindingContext());

        return analysisResult;
    }

    @NotNull
    public static PackageViewDescriptor extractObjCPackageFromAnalysisResult(@NotNull AnalysisResult analysisResult) {
        PackageViewDescriptor objcPackage =
                analysisResult.getModuleDescriptor().getPackage(ObjCPackageFragmentProvider.OBJC_PACKAGE_FQ_NAME);
        assert objcPackage != null : "Obj-C package wasn't resolved: " + analysisResult.getModuleDescriptor();
        return objcPackage;
    }

    @NotNull
    public static String runProcess(@NotNull String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();

            InputStreamReader output = new InputStreamReader(process.getInputStream());
            String result = CharStreams.toString(output);
            output.close();

            InputStreamReader errorStream = new InputStreamReader(process.getErrorStream());
            String error = CharStreams.toString(errorStream);
            errorStream.close();
            System.err.print(error);

            int exitCode = process.exitValue();
            assert exitCode == 0 : "Process exited with code " + exitCode + ", result: " + result;

            return result;
        }
        catch (Exception e) {
            throw UtilsPackage.rethrow(e);
        }
    }
}
