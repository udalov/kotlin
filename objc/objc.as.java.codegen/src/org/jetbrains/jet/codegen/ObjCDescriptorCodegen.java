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

package org.jetbrains.jet.codegen;

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.binding.CodegenBinding;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.objc.ObjCMetaclassDescriptor;
import org.jetbrains.jet.utils.UtilsPackage;
import org.jetbrains.org.objectweb.asm.Type;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class ObjCDescriptorCodegen {
    public static final String METACLASS_SUFFIX = "$metaclass";

    private final JetTypeMapper typeMapper;

    public ObjCDescriptorCodegen() {
        this.typeMapper = new JetTypeMapper(new BindingTraceContext(), ClassBuilderMode.FULL);
    }

    @NotNull
    public BindingContext getBindingContext() {
        return typeMapper.getBindingContext();
    }

    // This is needed to make JetTypeMapper correctly map class objects
    private void recordFQNForClassObject(@NotNull ClassDescriptor classDescriptor, @NotNull ClassDescriptor classObject) {
        String internalName = typeMapper.mapClass(classDescriptor).getInternalName();
        Type classObjectType = Type.getObjectType(internalName + JvmAbi.CLASS_OBJECT_SUFFIX);
        typeMapper.getBindingTrace().record(CodegenBinding.ASM_TYPE, classObject, classObjectType);
    }

    private void writeClassFile(@NotNull ClassDescriptor descriptor, @NotNull File outputDir, @NotNull byte[] bytes) {
        String internalName = typeMapper.mapClass(descriptor).getInternalName();
        File file = new File(outputDir, internalName + ".class");

        File outerDir = file.getParentFile();
        if (outerDir != null) {
            //noinspection ResultOfMethodCallIgnored
            outerDir.mkdirs();
        }

        try {
            FileUtil.writeToFile(file, bytes);
        }
        catch (IOException e) {
            throw UtilsPackage.rethrow(e);
        }
    }

    private void generateAndWriteClass(@NotNull File dylib, @NotNull File outputDir, @NotNull ClassDescriptor classDescriptor) {
        ObjCClassCodegen codegen = new ObjCClassCodegen(typeMapper, classDescriptor, dylib);
        byte[] bytes = codegen.generateClass();
        writeClassFile(classDescriptor, outputDir, bytes);
    }

    @NotNull
    private static Collection<ClassDescriptor> filterClasses(@NotNull Collection<DeclarationDescriptor> descriptors) {
        Collection<ClassDescriptor> result = new ArrayList<ClassDescriptor>(descriptors.size());
        for (DeclarationDescriptor descriptor : descriptors) {
            if (descriptor instanceof ClassDescriptor) {
                result.add((ClassDescriptor) descriptor);
            }
        }
        return result;
    }

    public void generate(@NotNull PackageViewDescriptor objcPackage, @NotNull File outputDir, @NotNull File dylib) {
        Collection<ClassDescriptor> classes = filterClasses(objcPackage.getMemberScope().getAllDescriptors());

        for (ClassDescriptor descriptor : classes) {
            if (descriptor instanceof ObjCMetaclassDescriptor) {
                String internalName = typeMapper.mapClass(((ObjCMetaclassDescriptor) descriptor).getClassDescriptor()).getInternalName();
                Type metaclassType = Type.getObjectType(internalName + METACLASS_SUFFIX);
                typeMapper.getBindingTrace().record(CodegenBinding.ASM_TYPE, descriptor, metaclassType);
            }
        }

        for (ClassDescriptor descriptor : classes) {
            generateAndWriteClass(dylib, outputDir, descriptor);

            ClassDescriptor classObject = descriptor.getClassObjectDescriptor();
            if (classObject != null) {
                recordFQNForClassObject(descriptor, classObject);
                generateAndWriteClass(dylib, outputDir, classObject);
            }
        }
    }
}
