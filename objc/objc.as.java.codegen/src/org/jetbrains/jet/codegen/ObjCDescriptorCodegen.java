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
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.objc.ObjCMetaclassDescriptor;
import org.jetbrains.jet.utils.UtilsPackage;
import org.jetbrains.org.objectweb.asm.Type;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class ObjCDescriptorCodegen {
    public static final String METACLASS_SUFFIX = "$metaclass";

    private final JetTypeMapper typeMapper;
    private final BindingTrace bindingTrace;

    public ObjCDescriptorCodegen() {
        this.bindingTrace = new BindingTraceContext();
        this.typeMapper = new JetTypeMapper(bindingTrace.getBindingContext(), ClassBuilderMode.FULL);
    }

    @NotNull
    public BindingContext getBindingContext() {
        return bindingTrace.getBindingContext();
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

    public void generate(@NotNull PackageViewDescriptor objcPackage, @NotNull File outputDir, @NotNull File dylib) {
        Collection<DeclarationDescriptor> allDescriptors = objcPackage.getMemberScope().getAllDescriptors();

        for (DeclarationDescriptor descriptor : allDescriptors) {
            if (descriptor instanceof ObjCMetaclassDescriptor) {
                ObjCMetaclassDescriptor metaclass = (ObjCMetaclassDescriptor) descriptor;
                String internalName = typeMapper.mapClass(metaclass.getClassDescriptor()).getInternalName();
                Type metaclassType = Type.getObjectType(internalName + METACLASS_SUFFIX);
                bindingTrace.record(CodegenBinding.ASM_TYPE, metaclass, metaclassType);
            }
        }

        for (DeclarationDescriptor descriptor : allDescriptors) {
            if (descriptor instanceof ClassDescriptor) {
                ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;

                generateAndWriteClass(dylib, outputDir, classDescriptor);

                ClassDescriptor classObject = classDescriptor.getClassObjectDescriptor();
                if (classObject != null) {
                    generateAndWriteClass(dylib, outputDir, classObject);
                }
            }
        }
    }
}
