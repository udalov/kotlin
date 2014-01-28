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

import jet.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.*;

import java.util.Collections;
import java.util.List;

public class ObjCBuiltIns {
    private static ObjCBuiltIns instance = null;

    public static void initialize(@NotNull DependencyClassByQualifiedNameResolver resolver) {
        // TODO: not very good, use injectors or something instead
        instance = new ObjCBuiltIns(resolver);
    }

    @NotNull
    public static ObjCBuiltIns getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Not initialized yet");
        }
        return instance;
    }

    private final ClassDescriptor pointerClass;
    private final ClassDescriptor objcObjectClass;
    private final ClassDescriptor objcClassClass;
    private final ClassDescriptor objcSelectorClass;

    private ObjCBuiltIns(@NotNull DependencyClassByQualifiedNameResolver resolver) {
        pointerClass = resolveClass(resolver, "Pointer");
        objcObjectClass = resolveClass(resolver, "ObjCObject");
        objcClassClass = resolveClass(resolver, "ObjCClass");
        objcSelectorClass = resolveClass(resolver, "ObjCSelector");
    }

    @NotNull
    private static ClassDescriptor resolveClass(@NotNull DependencyClassByQualifiedNameResolver resolver, @NotNull String className) {
        ClassDescriptor descriptor = resolver.resolveClass(new FqName("jet.objc." + className));
        assert descriptor != null : "Obj-C built-in class not found: " + className;
        return descriptor;
    }

    @NotNull
    private ObjCDeferredType pointerTo(@NotNull TypeProjection argument) {
        final List<TypeProjection> arguments = Collections.singletonList(argument);
        return new ObjCDeferredType(new Function0<JetType>() {
            @Override
            public JetType invoke() {
                return new JetTypeImpl(Annotations.EMPTY, pointerClass.getTypeConstructor(), false, arguments,
                                       pointerClass.getMemberScope(arguments));
            }
        });
    }

    @NotNull
    public JetType getPointerType(@NotNull JetType pointee) {
        return pointerTo(new TypeProjectionImpl(Variance.INVARIANT, pointee));
    }

    @NotNull
    public JetType getOpaquePointerType() {
        return pointerTo(SubstitutionUtils.makeStarProjection(pointerClass.getTypeConstructor().getParameters().get(0)));
    }

    @NotNull
    public ClassDescriptor getObjCObjectClass() {
        return objcObjectClass;
    }

    @NotNull
    public ClassDescriptor getObjCClassClass() {
        return objcClassClass;
    }

    @NotNull
    public ClassDescriptor getObjCSelectorClass() {
        return objcSelectorClass;
    }
}
