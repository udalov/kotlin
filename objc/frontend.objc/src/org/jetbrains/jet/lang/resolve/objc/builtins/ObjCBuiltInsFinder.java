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
import org.jetbrains.jet.descriptors.serialization.*;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentProvider;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.lang.BuiltInsSerializationUtil;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.storage.LockBasedStorageManager;
import org.jetbrains.jet.utils.UtilsPackage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

/* package */ class ObjCBuiltInsFinder extends AbstractDescriptorFinder {
    private final NameResolver nameResolver;

    public ObjCBuiltInsFinder(@NotNull PackageFragmentProvider packageFragmentProvider, @NotNull NameResolver nameResolver) {
        super(LockBasedStorageManager.NO_LOCKS, ObjCBuiltInsDeserializers.INSTANCE, packageFragmentProvider);
        this.nameResolver = nameResolver;
    }

    @Nullable
    @Override
    public ClassDescriptor findClass(@NotNull ClassId classId) {
        // TODO: this looks very strange here and should not be needed
        List<Name> segments = classId.asSingleFqName().pathSegments();
        if (segments.size() == 2 && segments.get(0).equals(KotlinBuiltIns.BUILT_INS_PACKAGE_NAME)) {
            ClassifierDescriptor classifier = KotlinBuiltIns.getInstance().getBuiltInsPackageScope().getClassifier(segments.get(1));
            if (classifier instanceof ClassDescriptor) {
                return (ClassDescriptor) classifier;
            }
        }

        return super.findClass(classId);
    }

    @Nullable
    @Override
    protected ClassData getClassData(@NotNull ClassId classId) {
        InputStream stream = ObjCBuiltInsPackageFragment.loadResourceNullable(BuiltInsSerializationUtil.getClassMetadataPath(classId));
        if (stream == null) return null;
        try {
            try {
                return new ClassData(nameResolver, ProtoBuf.Class.parseFrom(stream));
            }
            finally {
                stream.close();
            }
        }
        catch (IOException e) {
            throw UtilsPackage.rethrow(e);
        }
    }

    @NotNull
    @Override
    public Collection<Name> getClassNames(@NotNull FqName packageName) {
        // TODO
        throw new UnsupportedOperationException("Unsupported in Obj-C built-ins: " + packageName);
    }
}
