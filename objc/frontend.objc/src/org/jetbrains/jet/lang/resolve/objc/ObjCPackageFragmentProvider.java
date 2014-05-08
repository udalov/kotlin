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

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentProvider;
import org.jetbrains.jet.lang.descriptors.impl.MutablePackageFragmentDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.objc.builtins.ObjCBuiltIns;
import org.jetbrains.jet.utils.UtilsPackage;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.objc.ObjCIndex.TranslationUnit;

public class ObjCPackageFragmentProvider implements PackageFragmentProvider {
    public static final FqName OBJC_PACKAGE_FQ_NAME = new FqName("objc");

    private final PackageFragmentDescriptor objcPackage;

    public ObjCPackageFragmentProvider(@NotNull Project project, @NotNull ModuleDescriptor module, @NotNull ObjCBuiltIns objcBuiltIns) {
        String args = ObjCInteropParameters.getArgs(project);
        assert args != null : "Header parameter should be saved beforehand";

        TranslationUnit translationUnit = indexObjCHeaders(args);

        MutablePackageFragmentDescriptor objcPackage = new MutablePackageFragmentDescriptor(module, OBJC_PACKAGE_FQ_NAME);
        new ObjCDescriptorResolver(objcBuiltIns, objcPackage).processTranslationUnit(translationUnit);
        this.objcPackage = objcPackage;
    }

    static {
        System.loadLibrary("KotlinNativeIndexer");
    }

    private native byte[] buildObjCIndex(@NotNull String args);

    @NotNull
    private TranslationUnit indexObjCHeaders(@NotNull String args) {
        try {
            byte[] bytes = buildObjCIndex(args);
            return TranslationUnit.parseFrom(bytes);
        }
        catch (IOException e) {
            throw UtilsPackage.rethrow(e);
        }
    }

    @NotNull
    @Override
    public List<PackageFragmentDescriptor> getPackageFragments(@NotNull FqName fqName) {
        return fqName.equals(OBJC_PACKAGE_FQ_NAME) ?
               Collections.singletonList(objcPackage) :
               Collections.<PackageFragmentDescriptor>emptyList();
    }

    @NotNull
    @Override
    public Collection<FqName> getSubPackagesOf(@NotNull FqName fqName) {
        // TODO?
        return Collections.emptySet();
    }
}
