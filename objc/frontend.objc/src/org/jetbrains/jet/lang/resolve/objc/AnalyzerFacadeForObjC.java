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

import com.google.common.base.Predicate;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.analyzer.AnalyzerFacade;
import org.jetbrains.jet.context.ContextPackage;
import org.jetbrains.jet.context.GlobalContextImpl;
import org.jetbrains.jet.descriptors.serialization.descriptors.MemberFilter;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForObjC;
import org.jetbrains.jet.lang.descriptors.DependencyKind;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.java.mapping.JavaToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.objc.builtins.ObjCBuiltIns;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public enum AnalyzerFacadeForObjC implements AnalyzerFacade {

    INSTANCE;

    @NotNull
    @Override
    public Setup createSetup(@NotNull Project project, @NotNull Collection<JetFile> files) {
        // TODO
        throw new UnsupportedOperationException();
    }

    @NotNull
    public AnalyzeExhaust analyzeFiles(
            @NotNull Project project,
            @NotNull Collection<JetFile> files,
            @NotNull List<AnalyzerScriptParameter> scriptParameters,
            @NotNull Predicate<PsiFile> filesToAnalyzeCompletely
    ) {
        List<ImportPath> imports = new ArrayList<ImportPath>();
        // TODO: kotlin.objc.* or kotlin.jvm.objc.* ?
        imports.add(new ImportPath("jet.objc.*"));
        imports.addAll(AnalyzerFacadeForJVM.DEFAULT_IMPORTS);

        // TODO: shouldn't depend on Java, create another module for this analyzer facade (ObjC + JVM)
        ModuleDescriptorImpl module = new ModuleDescriptorImpl(Name.special("<module>"), imports, JavaToKotlinClassMap.getInstance());

        GlobalContextImpl global = ContextPackage.GlobalContext();
        BindingTrace trace = new BindingTraceContext();
        InjectorForTopDownAnalyzerForObjC injector = new InjectorForTopDownAnalyzerForObjC(
                project, global, new ObservableBindingTrace(trace), module, MemberFilter.ALWAYS_TRUE
        );

        module.addFragmentProvider(DependencyKind.SOURCES, injector.getObjCPackageFragmentProvider());
        module.addFragmentProvider(DependencyKind.BUILT_INS, ObjCBuiltIns.getInstance().getPackageFragmentProvider());
        module.addFragmentProvider(DependencyKind.BINARIES, injector.getJavaDescriptorResolver().getPackageFragmentProvider());

        TopDownAnalysisParameters topDownAnalysisParameters = TopDownAnalysisParameters.create(
                global.getStorageManager(), global.getExceptionTracker(), filesToAnalyzeCompletely, false, false
        );

        try {
            injector.getTopDownAnalyzer().analyzeFiles(topDownAnalysisParameters, files);
            return AnalyzeExhaust.success(trace.getBindingContext(), null, module);
        } finally {
            injector.destroy();
        }
    }
}
