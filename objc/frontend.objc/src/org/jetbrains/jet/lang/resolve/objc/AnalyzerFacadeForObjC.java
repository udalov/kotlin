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
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.analyzer.AnalyzerFacade;
import org.jetbrains.jet.context.ContextPackage;
import org.jetbrains.jet.context.GlobalContextImpl;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForObjC;
import org.jetbrains.jet.lang.descriptors.DependencyKind;
import org.jetbrains.jet.lang.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.java.mapping.JavaToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public enum AnalyzerFacadeForObjC implements AnalyzerFacade {

    INSTANCE;

    @NotNull
    @Override
    public Setup createSetup(@NotNull Project project, @NotNull Collection<JetFile> files, @NotNull GlobalSearchScope filesScope) {
        // TODO
        throw new UnsupportedOperationException();
    }

    @NotNull
    public static AnalyzeExhaust analyzeFiles(@NotNull Project project, @NotNull Collection<JetFile> files) {
        List<ImportPath> imports = new ArrayList<ImportPath>();
        imports.add(new ImportPath("kotlin.jvm.objc.*"));
        imports.addAll(AnalyzerFacadeForJVM.DEFAULT_IMPORTS);

        // TODO: shouldn't depend on Java, create another module for this analyzer facade (ObjC + JVM)
        ModuleDescriptorImpl module = new ModuleDescriptorImpl(Name.special("<module>"), imports, JavaToKotlinClassMap.getInstance());

        GlobalContextImpl global = ContextPackage.GlobalContext();
        BindingTrace trace = new BindingTraceContext();
        InjectorForTopDownAnalyzerForObjC injector = new InjectorForTopDownAnalyzerForObjC(
                project, global, new ObservableBindingTrace(trace), module
        );

        module.addFragmentProvider(DependencyKind.SOURCES, injector.getObjCPackageFragmentProvider());
        module.addFragmentProvider(DependencyKind.BINARIES, injector.getJavaDescriptorResolver().getPackageFragmentProvider());

        TopDownAnalysisParameters topDownAnalysisParameters = TopDownAnalysisParameters.create(
                global.getStorageManager(), global.getExceptionTracker(), Predicates.<PsiFile>alwaysTrue(), false, false
        );

        try {
            injector.getTopDownAnalyzer().analyzeFiles(topDownAnalysisParameters, files);
            return AnalyzeExhaust.success(trace.getBindingContext(), module);
        } finally {
            injector.destroy();
        }
    }
}
