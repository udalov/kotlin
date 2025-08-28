/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.types

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.runtime.structure.parameterizedTypeArguments
import org.jetbrains.kotlin.descriptors.runtime.structure.primitiveByWrapper
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.NewCapturedType
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.jvm.internal.*
import kotlin.reflect.jvm.jvmErasure

internal class KTypeImpl(
    val type: KotlinType,
    computeJavaType: (() -> Type)?,
    private val isAbbreviation: Boolean,
) : AbstractKType(computeJavaType) {
    constructor(type: KotlinType, computeJavaType: (() -> Type)? = null) : this(type, computeJavaType, isAbbreviation = false)

    override val classifier: KClassifier? by ReflectProperties.lazySoft { convert(type) }

    private fun convert(type: KotlinType): KClassifier? {
        if (isAbbreviation) {
            // Package scope in kotlin-reflect cannot load type aliases because it requires to know the file class where the typealias
            // is declared. Descriptor deserialization creates a "not found" MockClassDescriptor in this case.
            (type.constructor.declarationDescriptor as? NotFoundClasses.MockClassDescriptor)?.let { notFoundClass ->
                return KTypeAliasImpl(notFoundClass.fqNameSafe)
            }
        }
        when (val descriptor = type.constructor.declarationDescriptor) {
            is ClassDescriptor -> {
                val jClass = descriptor.toJavaClass() ?: return null
                if (KotlinBuiltIns.isArray(type)) {
                    val argument = type.arguments.singleOrNull()?.type ?: return KClassImpl(jClass)
                    // Make the array element type nullable to make sure that `kotlin.Array<Int>` is mapped to `[Ljava/lang/Integer;`
                    // instead of `[I`.
                    val elementClassifier =
                        convert(argument.makeNullable())
                            ?: throw KotlinReflectionInternalError("Cannot determine classifier for array element type: $this")
                    return KClassImpl(elementClassifier.jvmErasure.java.createArrayType())
                }

                if (!TypeUtils.isNullableType(type)) {
                    return KClassImpl(jClass.primitiveByWrapper ?: jClass)
                }

                return KClassImpl(jClass)
            }
            is TypeParameterDescriptor -> return KTypeParameterImpl(null, descriptor)
            else -> return null
        }
    }

    override val arguments: List<KTypeProjection> by ReflectProperties.lazySoft arguments@{
        val typeArguments = type.arguments
        if (typeArguments.isEmpty()) return@arguments emptyList()

        val parameterizedTypeArguments by lazy(PUBLICATION) { javaType!!.parameterizedTypeArguments }

        typeArguments.mapIndexed { i, typeProjection ->
            typeProjection.toKTypeProjection(if (computeJavaType == null) null else fun(): Type {
                return when (val javaType = javaType) {
                    is Class<*> -> {
                        // It's either an array or a raw type.
                        // TODO: return upper bound of the corresponding parameter for a raw type?
                        if (javaType.isArray) javaType.componentType else Any::class.java
                    }
                    is GenericArrayType -> {
                        if (i != 0) throw KotlinReflectionInternalError("Array type has been queried for a non-0th argument: $this")
                        javaType.genericComponentType
                    }
                    is ParameterizedType -> {
                        val argument = parameterizedTypeArguments[i]
                        // In "Foo<out Bar>", the JVM type of the first type argument should be "Bar", not "? extends Bar"
                        if (argument !is WildcardType) argument
                        else argument.lowerBounds.firstOrNull() ?: argument.upperBounds.first()
                    }
                    else -> throw KotlinReflectionInternalError("Non-generic type has been queried for arguments: $this")
                }
            })
        }
    }

    override val isMarkedNullable: Boolean
        get() = type.isMarkedNullable

    override val annotations: List<Annotation>
        get() = type.computeAnnotations()

    override fun makeNullableAsSpecified(nullable: Boolean): AbstractKType {
        // If the type is not marked nullable, it's either a non-null type or a platform type.
        if (!type.isFlexible() && isMarkedNullable == nullable) return this

        return KTypeImpl(TypeUtils.makeNullableAsSpecified(type, nullable), computeJavaType)
    }

    override fun makeDefinitelyNotNullAsSpecified(isDefinitelyNotNull: Boolean): AbstractKType {
        val result =
            if (isDefinitelyNotNull)
                DefinitelyNotNullType.makeDefinitelyNotNull(type.unwrap(), true) ?: type
            else
                (type as? DefinitelyNotNullType)?.original ?: type
        return KTypeImpl(result, computeJavaType)
    }

    override val abbreviation: KType?
        get() = type.getAbbreviation()?.let { KTypeImpl(it, computeJavaType, isAbbreviation = true) }

    override val isDefinitelyNotNullType: Boolean
        get() = type.isDefinitelyNotNullType

    override val isNothingType: Boolean
        get() = KotlinBuiltIns.isNothingOrNullableNothing(type)

    override val mutableCollectionClass: KClass<*>?
        get() {
            val classDescriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return null
            if (!JavaToKotlinClassMapper.isMutable(classDescriptor)) return null
            if (useLegacyImplementation) {
                return MutableCollectionKClass(
                    classifier as KClass<*>,
                    classDescriptor.fqNameSafe.asString(),
                    { container ->
                        classDescriptor.declaredTypeParameters.map { descriptor -> KTypeParameterImpl(container, descriptor) }
                    },
                    {
                        classDescriptor.typeConstructor.supertypes.map(::KTypeImpl)
                    },
                )
            }
            return getMutableCollectionKClass(classDescriptor.fqNameSafe, classifier as KClass<*>)
        }

    override val isSuspendFunctionType: Boolean
        get() = type.isSuspendFunctionType

    override val isRawType: Boolean
        get() = type is RawType

    override fun lowerBoundIfFlexible(): AbstractKType? =
        when (val unwrapped = type.unwrap()) {
            is FlexibleType -> KTypeImpl(unwrapped.lowerBound)
            else -> null
        }

    override fun upperBoundIfFlexible(): AbstractKType? =
        when (val unwrapped = type.unwrap()) {
            is FlexibleType -> KTypeImpl(unwrapped.upperBound)
            else -> null
        }

    override fun equals(other: Any?): Boolean =
        if (useLegacyImplementation) {
            other is KTypeImpl && type == other.type && classifier == other.classifier && arguments == other.arguments
        } else super.equals(other)

    override fun hashCode(): Int =
        if (useLegacyImplementation) {
            (31 * ((31 * type.hashCode()) + classifier.hashCode())) + arguments.hashCode()
        } else super.hashCode()
}

internal fun TypeProjection.toKTypeProjection(computeJavaType: (() -> Type)? = null): KTypeProjection {
    if (isStarProjection) return KTypeProjection.STAR

    val type = type
    val result = if (type is NewCapturedType) {
        CapturedKType(
            type.lowerType?.let(::KTypeImpl),
            CapturedKTypeConstructor(type.constructor.projection.toKTypeProjection()),
            type.isMarkedNullable,
        )
    } else {
        KTypeImpl(type, computeJavaType)
    }
    return when (projectionKind) {
        Variance.INVARIANT -> KTypeProjection.invariant(result)
        Variance.IN_VARIANCE -> KTypeProjection.contravariant(result)
        Variance.OUT_VARIANCE -> KTypeProjection.covariant(result)
    }
}
