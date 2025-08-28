/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

@file:JvmName("KClassifiers")

package kotlin.reflect.full

import org.jetbrains.kotlin.types.*
import kotlin.reflect.*
import kotlin.reflect.jvm.internal.KClassImpl
import kotlin.reflect.jvm.internal.KTypeParameterImpl
import kotlin.reflect.jvm.internal.KotlinReflectionInternalError
import kotlin.reflect.jvm.internal.types.KTypeImpl
import kotlin.reflect.jvm.internal.types.SimpleKType
import kotlin.reflect.jvm.internal.types.allTypeParameters
import kotlin.reflect.jvm.internal.useLegacyImplementation

/**
 * Creates a [KType] instance with the given classifier, type arguments, nullability and annotations.
 * If the number of passed type arguments is not equal to the total number of type parameters of a classifier,
 * an exception is thrown. If any of the arguments does not satisfy the bounds of the corresponding type parameter,
 * an exception is thrown.
 *
 * For classifiers representing type parameters, the type argument list must always be empty.
 * For classes, the type argument list should contain arguments for the type parameters of the class. If the class is `inner`,
 * the list should follow with arguments for the type parameters of its outer class, and so forth until a class is
 * not `inner`, or is declared on the top level.
 */
@SinceKotlin("1.1")
fun KClassifier.createType(
    arguments: List<KTypeProjection> = emptyList(),
    nullable: Boolean = false,
    annotations: List<Annotation> = emptyList(),
): KType {
    return createTypeImpl(arguments, nullable, annotations)
}

internal fun KClassifier.createTypeImpl(
    arguments: List<KTypeProjection> = emptyList(),
    nullable: Boolean = false,
    annotations: List<Annotation> = emptyList(),
    mutableCollectionClass: KClass<*>? = null,
): KType {
    if (useLegacyImplementation) {
        val descriptor = when (this) {
            is KClassImpl<*> -> descriptor
            is KTypeParameterImpl -> descriptor
            else -> null
        } ?: throw KotlinReflectionInternalError("Cannot create type for an unsupported classifier: $this (${this.javaClass})")

        checkArgumentsSize(descriptor.typeConstructor.parameters.size, arguments.size)

        return KTypeImpl(createKotlinType(descriptor.typeConstructor, arguments, nullable))
    }

    val parameters = (this as? KClass<*>)?.allTypeParameters().orEmpty()
    checkArgumentsSize(parameters.size, arguments.size)

    // TODO: throw exception if argument does not satisfy bounds

    return SimpleKType(
        this,
        arguments,
        nullable,
        annotations,
        abbreviation = null,
        isDefinitelyNotNullType = false,
        isNothingType = false,
        isSuspendFunctionType = false,
        mutableCollectionClass,
    )
}

private fun checkArgumentsSize(parametersSize: Int, argumentsSize: Int) {
    if (parametersSize != argumentsSize) {
        throw IllegalArgumentException("Class declares $parametersSize type parameters, but $argumentsSize were provided.")
    }
}

private fun createKotlinType(typeConstructor: TypeConstructor, arguments: List<KTypeProjection>, nullable: Boolean): SimpleType {
    val parameters = typeConstructor.parameters
    return KotlinTypeFactory.simpleType(TypeAttributes.Empty, typeConstructor, arguments.mapIndexed { index, typeProjection ->
        val type = (typeProjection.type as KTypeImpl?)?.type
        when (typeProjection.variance) {
            KVariance.INVARIANT -> TypeProjectionImpl(Variance.INVARIANT, type!!)
            KVariance.IN -> TypeProjectionImpl(Variance.IN_VARIANCE, type!!)
            KVariance.OUT -> TypeProjectionImpl(Variance.OUT_VARIANCE, type!!)
            null -> StarProjectionImpl(parameters[index])
        }
    }, nullable)
}

/**
 * Creates an instance of [KType] with the given classifier, substituting all its type parameters with star projections.
 * The resulting type is not marked as nullable and does not have any annotations.
 *
 * @see [KClassifier.createType]
 */
@SinceKotlin("1.1")
val KClassifier.starProjectedType: KType
    get() {
        val descriptor = (this as? KClassImpl<*>)?.descriptor
            ?: return createType()

        val typeParameters = descriptor.typeConstructor.parameters
        if (typeParameters.isEmpty()) return createType() // TODO: optimize, get defaultType from ClassDescriptor

        return createType(typeParameters.map { KTypeProjection.STAR })
    }
