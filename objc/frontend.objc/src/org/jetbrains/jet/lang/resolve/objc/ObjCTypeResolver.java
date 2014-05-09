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

import com.intellij.util.containers.ContainerUtil;
import kotlin.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.objc.builtins.ObjCBuiltIns;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ObjCTypeResolver {
    private final ObjCBuiltIns objcBuiltIns;
    private final PackageFragmentDescriptor objcPackage;
    private volatile Map<String, JetType> builtInTypes;

    public ObjCTypeResolver(@NotNull ObjCBuiltIns objcBuiltIns, @NotNull PackageFragmentDescriptor objcPackage) {
        this.objcBuiltIns = objcBuiltIns;
        this.objcPackage = objcPackage;
    }

    private Map<String, JetType> getBuiltInTypesMap() {
        if (builtInTypes == null) {
            KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
            builtInTypes = new ContainerUtil.ImmutableMapBuilder<String, JetType>()
                    .put("V", builtIns.getUnitType())
                    .put("UC", builtIns.getCharType())
                    .put("US", builtIns.getShortType())
                    .put("UI", builtIns.getIntType())
                    .put("UJ", builtIns.getLongType())
                    .put("C", builtIns.getCharType())
                    .put("Z", builtIns.getBooleanType())
                    .put("S", builtIns.getShortType())
                    .put("I", builtIns.getIntType())
                    .put("J", builtIns.getLongType())
                    .put("F", builtIns.getFloatType())
                    .put("D", builtIns.getDoubleType())
                    .put("OI", objcBuiltIns.getObjCObjectClass().getDefaultType())
                    .put("OC", objcBuiltIns.getObjCClassClass().getDefaultType())
                    .put("OS", objcBuiltIns.getObjCSelectorClass().getDefaultType())
                    .build();
        }
        return builtInTypes;
    }

    @NotNull
    public JetType createTypeForClass(@NotNull Name className) {
        return new ObjCClassType(objcPackage, className);
    }

    @NotNull
    private static JetType createFunctionType(@NotNull final List<JetType> paramTypes, @NotNull final JetType returnType) {
        if (paramTypes.size() > KotlinBuiltIns.FUNCTION_TRAIT_COUNT) {
            throw new UnsupportedOperationException("Function types with more than " + KotlinBuiltIns.FUNCTION_TRAIT_COUNT +
                                                    " parameters are not supported");
        }

        return new ObjCDeferredType(new Function0<JetType>() {
            @Override
            public JetType invoke() {
                return KotlinBuiltIns.getInstance().getFunctionType(Annotations.EMPTY, /* receiverType */ null, paramTypes, returnType);
            }
        });
    }

    private class TypeParser {
        private final String type;
        private int at;

        public TypeParser(@NotNull String type) {
            this.type = type;
            this.at = 0;
        }

        private boolean at(@NotNull String s) {
            return type.substring(at).startsWith(s);
        }

        private void expect(@NotNull String s) {
            if (!advance(s)) error("Expecting <" + s + "> (at=" + at + ")");
        }

        private boolean advance(@NotNull String s) {
            if (at(s)) {
                at += s.length();
                return true;
            }
            return false;
        }

        private void error(@NotNull String s) {
            throw new IllegalStateException(s + ": " + type);
        }

        @NotNull
        public JetType parse() {
            if (at == type.length()) error("No type to parse");

            for (Map.Entry<String, JetType> entry : getBuiltInTypesMap().entrySet()) {
                if (advance(entry.getKey())) return entry.getValue();
            }

            if (advance("L")) {
                int semicolon = type.indexOf(';', at);
                if (semicolon < 0) error("L without a matching semicolon");
                String className = type.substring(at, semicolon);
                expect(className);
                expect(";");
                // TODO: for some reason Clang doesn't index forward declaration of the class named 'Protocol' defined in objc/Protocol.h
                if ("Protocol".equals(className)) return KotlinBuiltIns.getInstance().getNullableAnyType();
                return createTypeForClass(Name.identifier(className));
            }

            if (advance("*(")) {
                List<JetType> paramTypes = new ArrayList<JetType>();
                while (!advance(")")) {
                    if (advance(".")) {
                        // TODO: support vararg
                        continue;
                    }
                    paramTypes.add(parse());
                }
                JetType returnType = parse();
                expect(";");
                return createFunctionType(paramTypes, returnType);
            }

            if (advance("*V;")) {
                // Special case for "void *"
                return objcBuiltIns.getOpaquePointerType();
            }

            if (advance("*")) {
                JetType pointee = parse();
                expect(";");
                return objcBuiltIns.getPointerType(pointee);
            }

            if (at("X(")) {
                at = type.indexOf(')', at) + 1;
            }
            else {
                throw new UnsupportedOperationException("Unsupported type (at=" + at + "): " + type);
            }

            return KotlinBuiltIns.getInstance().getAnyType();
        }
    }

    @NotNull
    public JetType resolveType(@NotNull String type) {
        return new TypeParser(type).parse();
    }
}
