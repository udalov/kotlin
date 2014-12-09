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

package org.jetbrains.jet.lang.resolve.objc;

import com.google.common.base.Predicates;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalysisResult;
import org.jetbrains.jet.context.ContextPackage;
import org.jetbrains.jet.context.GlobalContextImpl;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForObjC;
import org.jetbrains.jet.lang.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.java.TopDownAnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.java.mapping.JavaToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.lazy.declarations.FileBasedDeclarationProviderFactory;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.*;

public class AnalyzerFacadeForObjC {
    @NotNull
    public static AnalysisResult analyzeFiles(@NotNull Project project, @NotNull Collection<JetFile> files) {
        List<ImportPath> imports = new ArrayList<ImportPath>();
        imports.add(new ImportPath("kotlin.jvm.objc.*"));
        imports.addAll(TopDownAnalyzerFacadeForJVM.DEFAULT_IMPORTS);

        // TODO: shouldn't depend on Java, create another module for this analyzer facade (ObjC + JVM)
        ModuleDescriptorImpl module = new ModuleDescriptorImpl(Name.special("<module>"), imports, JavaToKotlinClassMap.INSTANCE);
        module.addDependencyOnModule(KotlinBuiltIns.getInstance().getBuiltInsModule());
        module.addDependencyOnModule(module);

        GlobalContextImpl global = ContextPackage.GlobalContext();
        BindingTrace trace = new BindingTraceContext();
        FileBasedDeclarationProviderFactory declarationProviderFactory =
                new FileBasedDeclarationProviderFactory(global.getStorageManager(), files);
        InjectorForTopDownAnalyzerForObjC injector = new InjectorForTopDownAnalyzerForObjC(
                project, global, new ObservableBindingTrace(trace), module, declarationProviderFactory
        );

        TopDownAnalysisParameters topDownAnalysisParameters = TopDownAnalysisParameters.create(
                global.getStorageManager(), global.getExceptionTracker(), Predicates.<PsiFile>alwaysTrue(), false, false
        );

        try {
            injector.getLazyTopDownAnalyzer().analyzeFiles(topDownAnalysisParameters, files, Arrays.asList(
                    injector.getJavaDescriptorResolver().getPackageFragmentProvider(),
                    injector.getObjCPackageFragmentProvider()
            ));
            return AnalysisResult.success(trace.getBindingContext(), module);
        }
        finally {
            injector.destroy();
        }
    }
}
