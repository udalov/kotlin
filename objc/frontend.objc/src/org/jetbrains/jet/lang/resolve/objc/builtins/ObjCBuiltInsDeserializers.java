/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.NameResolver;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.descriptors.serialization.descriptors.AnnotationDeserializer;
import org.jetbrains.jet.descriptors.serialization.descriptors.ConstantDeserializer;
import org.jetbrains.jet.descriptors.serialization.descriptors.Deserializers;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassOrPackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;

/* package */ class ObjCBuiltInsDeserializers implements Deserializers, AnnotationDeserializer, ConstantDeserializer {
    public static final ObjCBuiltInsDeserializers INSTANCE = new ObjCBuiltInsDeserializers();

    @NotNull
    @Override
    public Annotations loadClassAnnotations(@NotNull ClassDescriptor descriptor, @NotNull ProtoBuf.Class classProto) {
        // TODO
        return Annotations.EMPTY;
    }

    @NotNull
    @Override
    public Annotations loadCallableAnnotations(
            @NotNull ClassOrPackageFragmentDescriptor container,
            @NotNull ProtoBuf.Callable proto,
            @NotNull NameResolver nameResolver,
            @NotNull Deserializers.AnnotatedCallableKind kind
    ) {
        // TODO
        return Annotations.EMPTY;
    }

    @NotNull
    @Override
    public Annotations loadValueParameterAnnotations(
            @NotNull ClassOrPackageFragmentDescriptor container,
            @NotNull ProtoBuf.Callable callable,
            @NotNull NameResolver nameResolver,
            @NotNull Deserializers.AnnotatedCallableKind kind,
            @NotNull ProtoBuf.Callable.ValueParameter proto
    ) {
        // TODO
        return Annotations.EMPTY;
    }

    @Nullable
    @Override
    public CompileTimeConstant<?> loadPropertyConstant(
            @NotNull ClassOrPackageFragmentDescriptor container,
            @NotNull ProtoBuf.Callable proto,
            @NotNull NameResolver nameResolver,
            @NotNull AnnotatedCallableKind kind
    ) {
        // TODO
        return null;
    }

    @NotNull
    @Override
    public AnnotationDeserializer getAnnotationDeserializer() {
        return this;
    }

    @NotNull
    @Override
    public ConstantDeserializer getConstantDeserializer() {
        return this;
    }
}
