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

package org.jetbrains.jet.lang.resolve.objc.builtins;

import kotlin.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.PackageFragmentProvider;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.objc.ObjCDeferredType;
import org.jetbrains.jet.lang.types.*;

import java.util.Collections;
import java.util.List;

public class ObjCBuiltIns {
    public static final FqName FQ_NAME = new FqName("jet.objc");

    private static ObjCBuiltIns instance = null;

    @NotNull
    public static ObjCBuiltIns getInstance() {
        // TODO: synchronized?
        if (instance == null) {
            instance = new ObjCBuiltIns();
        }
        return instance;
    }

    private final ObjCBuiltInsPackageFragment objcPackage;

    private ObjCBuiltIns() {
        ModuleDescriptorImpl module = new ModuleDescriptorImpl(
                Name.special("<objc built-ins module>"),
                Collections.<ImportPath>emptyList(),
                PlatformToKotlinClassMap.EMPTY
        );

        objcPackage = new ObjCBuiltInsPackageFragment(module);
    }

    @NotNull
    public PackageFragmentProvider getPackageFragmentProvider() {
        return objcPackage.getProvider();
    }

    @NotNull
    private ObjCDeferredType pointerTo(@NotNull TypeProjection argument) {
        final List<TypeProjection> arguments = Collections.singletonList(argument);
        return new ObjCDeferredType(new Function0<JetType>() {
            @Override
            public JetType invoke() {
                return new JetTypeImpl(Annotations.EMPTY, getPointerClass().getTypeConstructor(), false, arguments,
                                       getPointerClass().getMemberScope(arguments));
            }
        });
    }

    @NotNull
    public JetType getPointerType(@NotNull JetType pointee) {
        return pointerTo(new TypeProjectionImpl(Variance.INVARIANT, pointee));
    }

    @NotNull
    public JetType getOpaquePointerType() {
        return pointerTo(TypeUtils.makeStarProjection(getPointerClass().getTypeConstructor().getParameters().get(0)));
    }

    @NotNull
    private ClassDescriptor classByName(@NotNull String name) {
        ClassifierDescriptor found = objcPackage.getMemberScope().getClassifier(Name.identifier(name));
        if (!(found instanceof ClassDescriptor)) {
            throw new IllegalStateException("Obj-C built-in class not found: " + name);
        }
        return (ClassDescriptor) found;
    }

    @NotNull
    public ClassDescriptor getPointerClass() {
        return classByName("Pointer");
    }

    @NotNull
    public ClassDescriptor getObjCObjectClass() {
        return classByName("ObjCObject");
    }

    @NotNull
    public ClassDescriptor getObjCClassClass() {
        return classByName("ObjCClass");
    }

    @NotNull
    public ClassDescriptor getObjCSelectorClass() {
        return classByName("ObjCSelector");
    }
}
