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
import org.jetbrains.jet.descriptors.serialization.NameSerializationUtil;
import org.jetbrains.jet.descriptors.serialization.PackageData;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedPackageMemberScope;
import org.jetbrains.jet.descriptors.serialization.descriptors.MemberFilter;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.PackageFragmentProvider;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.lang.BuiltInsSerializationUtil;
import org.jetbrains.jet.storage.LockBasedStorageManager;
import org.jetbrains.jet.utils.UtilsPackage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/* package */ class ObjCBuiltInsPackageFragment extends PackageFragmentDescriptorImpl implements PackageFragmentDescriptor {
    private final PackageFragmentProvider provider;
    private final JetScope scope;

    public ObjCBuiltInsPackageFragment(@NotNull ModuleDescriptor module) {
        super(module, ObjCBuiltIns.FQ_NAME);
        this.provider = new ObjCBuiltInsPackageFragmentProvider();

        NameResolver nameResolver;
        ProtoBuf.Package packageProto;
        try {
            InputStream nameResolverStream = loadResource(BuiltInsSerializationUtil.getNameTableFilePath(ObjCBuiltIns.FQ_NAME));
            InputStream packageProtoStream = loadResource(BuiltInsSerializationUtil.getPackageFilePath(ObjCBuiltIns.FQ_NAME));
            try {
                nameResolver = NameSerializationUtil.deserializeNameResolver(nameResolverStream);
                packageProto = ProtoBuf.Package.parseFrom(packageProtoStream);
            }
            finally {
                nameResolverStream.close();
                packageProtoStream.close();
            }
        }
        catch (IOException e) {
            throw UtilsPackage.rethrow(e);
        }

        this.scope = new DeserializedPackageMemberScope(LockBasedStorageManager.NO_LOCKS, this, ObjCBuiltInsDeserializers.INSTANCE,
                                                        MemberFilter.ALWAYS_TRUE, new ObjCBuiltInsFinder(provider, nameResolver),
                                                        new PackageData(nameResolver, packageProto));
    }

    @NotNull
    public static InputStream loadResource(@NotNull String path) {
        InputStream stream = loadResourceNullable(path);
        if (stream == null) {
            throw new IllegalStateException("Resource not found in classpath: " + path);
        }
        return stream;
    }

    @Nullable
    public static InputStream loadResourceNullable(@NotNull String path) {
        return ObjCBuiltIns.class.getClassLoader().getResourceAsStream(path);
    }

    @NotNull
    @Override
    public JetScope getMemberScope() {
        return scope;
    }

    @NotNull
    public PackageFragmentProvider getProvider() {
        return provider;
    }

    private class ObjCBuiltInsPackageFragmentProvider implements PackageFragmentProvider {
        @NotNull
        @Override
        public List<PackageFragmentDescriptor> getPackageFragments(@NotNull FqName fqName) {
            if (fqName.equals(ObjCBuiltIns.FQ_NAME)) {
                return Collections.<PackageFragmentDescriptor>singletonList(ObjCBuiltInsPackageFragment.this);
            }
            return Collections.emptyList();
        }

        @NotNull
        @Override
        public Collection<FqName> getSubPackagesOf(@NotNull FqName fqName) {
            // TODO
            return Collections.emptyList();
        }
    }
}
