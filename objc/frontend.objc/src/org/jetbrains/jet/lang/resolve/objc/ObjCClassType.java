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

import kotlin.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;

public class ObjCClassType extends ObjCDeferredType {
    private final Name className;

    public ObjCClassType(@NotNull final PackageFragmentDescriptor objcPackage, @NotNull final Name className) {
        super(new Function0<JetType>() {
            @Override
            public JetType invoke() {
                ClassifierDescriptor classifier = objcPackage.getMemberScope().getClassifier(className);
                assert classifier != null : "Objective-C class is not yet resolved: " + className;
                return classifier.getDefaultType();
            }
        });

        this.className = className;
    }

    @NotNull
    public Name getClassName() {
        return className;
    }
}
