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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptor;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;
import java.util.Collections;

// TODO: get rid of MutableClassDescriptor inheritance
public class ObjCClassDescriptor extends MutableClassDescriptor {
    private final Collection<JetType> lazySupertypes;

    public ObjCClassDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull ClassKind kind,
            @NotNull Modality modality,
            @NotNull Name name,
            @NotNull Collection<JetType> supertypes
    ) {
        super(containingDeclaration, scope(containingDeclaration), kind, false, name);

        setModality(modality);
        setVisibility(Visibilities.PUBLIC);

        WritableScopeImpl scope = new WritableScopeImpl(JetScope.EMPTY, this, RedeclarationHandler.THROW_EXCEPTION, "Obj-C class");
        scope.changeLockLevel(WritableScope.LockLevel.BOTH);
        setScopeForMemberLookup(scope);
        setTypeParameterDescriptors(Collections.<TypeParameterDescriptor>emptyList());

        this.lazySupertypes = supertypes;

        createTypeConstructor();
    }

    @NotNull
    private static JetScope scope(@NotNull DeclarationDescriptor containingDeclaration) {
        if (containingDeclaration instanceof PackageFragmentDescriptor) {
            return ((PackageFragmentDescriptor) containingDeclaration).getMemberScope();
        }
        if (containingDeclaration instanceof ClassDescriptor) {
            return ((ClassDescriptor) containingDeclaration).getDefaultType().getMemberScope();
        }
        throw new UnsupportedOperationException("Obj-C class container not supported: " + containingDeclaration);
    }

    /* package */ void initialize() {
        // Initialization is a separate step, because addSupertype method actually computes deferred types.
        // Not all supertypes may be available at the time of constructing this class
        // TODO: fix addSupertype
        for (JetType supertype : lazySupertypes) {
            addSupertype(supertype);
        }
    }

    /* package */ Collection<JetType> getLazySupertypes() {
        return lazySupertypes;
    }

    @Nullable
    @Override
    public ObjCClassDescriptor getClassObjectDescriptor() {
        return (ObjCClassDescriptor) super.getClassObjectDescriptor();
    }

    @Override
    public void lockScopes() {
        // TODO
    }
}
