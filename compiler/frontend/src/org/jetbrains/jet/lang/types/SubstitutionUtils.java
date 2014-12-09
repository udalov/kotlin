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

package org.jetbrains.jet.lang.types;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.List;
import java.util.Map;

public class SubstitutionUtils {
    private SubstitutionUtils() {
    }

    /**
     * Builds a context with all the supertypes' parameters substituted
     */
    @NotNull
    public static TypeSubstitutor buildDeepSubstitutor(@NotNull JetType type) {
        Map<TypeConstructor, TypeProjection> substitution = Maps.newHashMap();
        TypeSubstitutor typeSubstitutor = TypeSubstitutor.create(substitution);
        // we use the mutability of the map here
        fillInDeepSubstitutor(type, typeSubstitutor, substitution, null);
        return typeSubstitutor;
    }

    /**
      For each supertype of a given type, we map type parameters to type arguments.

      For instance, we have the following class hierarchy:
          trait Iterable<out T>
          trait Collection<out E>: Iterable<E>
          trait MyFooCollection<F>: Collection<Foo<F>>

      For MyFooCollection<out CharSequence>, the following multimap will be returned:
          T declared in Iterable -> Foo<out CharSequence>
          E declared in Collection -> Foo<out CharSequence>
          F declared in MyFooCollection -> out CharSequence
     */
    @NotNull
    public static Multimap<TypeConstructor, TypeProjection> buildDeepSubstitutionMultimap(@NotNull JetType type) {
        Multimap<TypeConstructor, TypeProjection> fullSubstitution = LinkedHashMultimap.create();
        Map<TypeConstructor, TypeProjection> substitution = Maps.newHashMap();
        TypeSubstitutor typeSubstitutor = TypeSubstitutor.create(substitution);
        // we use the mutability of the map here
        fillInDeepSubstitutor(type, typeSubstitutor, substitution, fullSubstitution);
        return fullSubstitution;
    }

    // we use the mutability of the substitution map here
    private static void fillInDeepSubstitutor(
            @NotNull JetType context,
            @NotNull TypeSubstitutor substitutor,
            @NotNull Map<TypeConstructor, TypeProjection> substitution,
            @Nullable Multimap<TypeConstructor, TypeProjection> fullSubstitution
    ) {
        List<TypeParameterDescriptor> parameters = context.getConstructor().getParameters();
        List<TypeProjection> arguments = context.getArguments();

        if (parameters.size() != arguments.size()) {
            throw new IllegalStateException();
        }

        for (int i = 0; i < arguments.size(); i++) {
            TypeProjection argument = arguments.get(i);
            TypeParameterDescriptor parameter = parameters.get(i);

            TypeProjection substitute = substitutor.substitute(argument);
            assert substitute != null;
            substitution.put(parameter.getTypeConstructor(), substitute);
            if (fullSubstitution != null) {
                fullSubstitution.put(parameter.getTypeConstructor(), substitute);
            }
        }
        if (KotlinBuiltIns.isNothingOrNullableNothing(context)) return;
        for (JetType supertype : context.getConstructor().getSupertypes()) {
            fillInDeepSubstitutor(supertype, substitutor, substitution, fullSubstitution);
        }
    }
}
