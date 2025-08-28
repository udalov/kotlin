/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.types

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.typeUtil.builtIns
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.internal.KotlinReflectionInternalError
import kotlin.reflect.jvm.internal.useLegacyImplementation

internal fun createPlatformKType(lowerBound: KType, upperBound: KType): KType =
    if (useLegacyImplementation)
        KTypeImpl(
            KotlinTypeFactory.flexibleType(
                (lowerBound as KTypeImpl).type as SimpleType,
                (upperBound as KTypeImpl).type as SimpleType,
            )
        )
    else
        FlexibleKType.create(
            lowerBound as AbstractKType,
            upperBound as AbstractKType,
            // TODO: KT-78951 typeOf creates a non-raw type for raw types from Java
            isRawType = false,
        )

internal fun createMutableCollectionKType(type: KType): KType {
    if (useLegacyImplementation) {
        val kotlinType = (type as KTypeImpl).type
        require(kotlinType is SimpleType) { "Non-simple type cannot be a mutable collection type: $type" }
        val classifier = kotlinType.constructor.declarationDescriptor as? ClassDescriptor
            ?: throw IllegalArgumentException("Non-class type cannot be a mutable collection type: $type")
        return KTypeImpl(
            KotlinTypeFactory.simpleType(kotlinType, constructor = classifier.readOnlyToMutable().typeConstructor)
        )
    }

    val readonlyClass = (type as SimpleKType).classifier
    val readonlyFqName = (readonlyClass as? KClass<*>)?.qualifiedName
        ?: throw KotlinReflectionInternalError("Non-class type cannot be a mutable collection type: $type")
    val mutableFqName = JavaToKotlinClassMap.readOnlyToMutable(FqNameUnsafe(readonlyFqName))
        ?: throw IllegalArgumentException("Not a readonly collection: $type")

    return SimpleKType(
        type.classifier,
        type.arguments,
        type.isMarkedNullable,
        type.annotations,
        type.abbreviation,
        type.isDefinitelyNotNullType,
        type.isNothingType,
        type.isSuspendFunctionType,
        getMutableCollectionKClass(mutableFqName, readonlyClass),
    )
}

private fun ClassDescriptor.readOnlyToMutable(): ClassDescriptor {
    val fqName = JavaToKotlinClassMap.readOnlyToMutable(fqNameUnsafe)
        ?: throw IllegalArgumentException("Not a readonly collection: $this")
    return builtIns.getBuiltInClassByFqName(fqName)
}

internal fun createNothingType(type: KType): KType {
    if (useLegacyImplementation) {
        val kotlinType = (type as KTypeImpl).type
        require(kotlinType is SimpleType) { "Non-simple type cannot be a Nothing type: $type" }
        return KTypeImpl(
            KotlinTypeFactory.simpleType(kotlinType, constructor = kotlinType.builtIns.nothing.typeConstructor)
        )
    }

    type as SimpleKType
    return SimpleKType(
        type.classifier,
        type.arguments,
        type.isMarkedNullable,
        type.annotations,
        type.abbreviation,
        type.isDefinitelyNotNullType,
        isNothingType = true,
        type.isSuspendFunctionType,
        type.mutableCollectionClass,
    )
}
