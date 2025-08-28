/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.lang.reflect.Type
import kotlin.metadata.*
import kotlin.metadata.jvm.annotations
import kotlin.reflect.*
import kotlin.reflect.jvm.internal.calls.createAnnotationInstance
import kotlin.reflect.jvm.internal.types.MutableCollectionKClass
import kotlin.reflect.jvm.internal.types.SimpleKType
import kotlin.reflect.jvm.internal.types.getMutableCollectionKClass
import kotlin.reflect.jvm.jvmErasure

internal fun ClassName.toClassId(): ClassId {
    val isLocal = startsWith(".")
    val fullName = if (isLocal) substring(1) else this
    return ClassId(
        FqName(fullName.substringBeforeLast('/', "").replace('/', '.')),
        FqName(fullName.substringAfterLast('/')),
        isLocal,
    )
}

internal fun ClassName.toNonLocalSimpleName(): String {
    require(!startsWith(".")) { "Local class is not supported: $this" }
    return substringAfterLast('/').substringAfterLast('.')
}

internal fun ClassLoader.loadKClass(name: ClassName): KClass<*>? =
    loadClass(name.toClassId())?.kotlin

internal class TypeParameterTable(
    private val map: Map<Int, KTypeParameter>,
    private val parent: TypeParameterTable?,
) {
    operator fun get(id: Int): KTypeParameter? = map[id] ?: parent?.get(id)

    companion object {
        @JvmField
        val EMPTY = TypeParameterTable(emptyMap(), null)
    }
}

internal fun KmType.toKType(
    classLoader: ClassLoader,
    typeParameterTable: TypeParameterTable,
    computeJavaType: (() -> Type)? = null,
): KType {
    val arguments = generateSequence(this) { it.outerType }
        .flatMap { it.arguments }
        .map { it.toKTypeProjection(classLoader, typeParameterTable) }
        .toList()
    val kClassifier = classifier.toClassifier(classLoader, typeParameterTable, arguments)
    return SimpleKType(
        kClassifier,
        arguments,
        isNullable,
        annotations.map { it.toAnnotation(classLoader) },
        abbreviatedType?.toKType(classLoader, typeParameterTable),
        isDefinitelyNonNull,
        (classifier as? KmClassifier.Class)?.name == "kotlin/Nothing",
        isSuspend,
        classifier.toMutableCollectionKClass(kClassifier),
        computeJavaType,
    )
}

private fun KmClassifier.toClassifier(
    classLoader: ClassLoader, typeParameterTable: TypeParameterTable, typeArguments: List<KTypeProjection>,
): KClassifier = when (this) {
    is KmClassifier.Class ->
        if (name == "kotlin/Array")
            (typeArguments.single().type ?: StandardKTypes.ANY).jvmErasure.java.createArrayType().kotlin
        else
            classLoader.loadKClass(name) ?: throw KotlinReflectionInternalError("Class not found: $name")
    is KmClassifier.TypeAlias ->
        error("Type alias is not supported as a classifier of a type: $name")
    is KmClassifier.TypeParameter ->
        typeParameterTable[id] ?: throw KotlinReflectionInternalError("Type parameter not found: $id")
}

private fun KmTypeProjection.toKTypeProjection(classLoader: ClassLoader, typeParameterTable: TypeParameterTable): KTypeProjection =
    if (this == KmTypeProjection.STAR)
        KTypeProjection.STAR
    else
        KTypeProjection(variance?.toKVariance(), type?.toKType(classLoader, typeParameterTable))

private fun KmVariance.toKVariance(): KVariance = when (this) {
    KmVariance.IN -> KVariance.IN
    KmVariance.OUT -> KVariance.OUT
    KmVariance.INVARIANT -> KVariance.INVARIANT
}

private fun KmClassifier.toMutableCollectionKClass(kClassifier: KClassifier): MutableCollectionKClass<*>? {
    val classId = (this as? KmClassifier.Class)?.name?.toClassId() ?: return null
    if (!JavaToKotlinClassMap.isMutable(classId)) return null
    return getMutableCollectionKClass(classId.asSingleFqName(), kClassifier as KClass<*>)
}

private fun KmAnnotation.toAnnotation(classLoader: ClassLoader): Annotation =
    createAnnotationInstance(
        classLoader.loadClass(className.toClassId())
            ?: throw KotlinReflectionInternalError("Annotation class not found: $className"),
        arguments.mapValues { (name, arg) -> arg.toAnnotationArgument(className, name, classLoader) },
    ) as Annotation

private fun KmAnnotationArgument.toAnnotationArgument(
    annotationClassName: ClassName, argumentName: String?, classLoader: ClassLoader,
): Any = when (this) {
    is KmAnnotationArgument.AnnotationValue -> annotation.toAnnotation(classLoader)
    is KmAnnotationArgument.ArrayKClassValue -> {
        var klass = classLoader.loadKClass(className)?.java
            ?: throw KotlinReflectionInternalError("Unresolved class: $className")
        repeat(arrayDimensionCount) {
            klass = klass.createArrayType()
        }
        klass
    }
    is KmAnnotationArgument.ArrayValue -> {
        // We need to create an array of a correct type, and for that we need to look up the type of the corresponding annotation parameter.
        val annotation = classLoader.loadKClass(annotationClassName)?.takeIf { it.java.isAnnotation }
            ?: throw KotlinReflectionInternalError("Not an annotation class: $annotationClassName")
        val parameterType = annotation.constructors.singleOrNull()?.parameters?.singleOrNull { it.name == argumentName }?.type
            ?: throw KotlinReflectionInternalError("No parameter $argumentName found in annotation constructor of $annotationClassName")
        val arrayClass = (parameterType.classifier as? KClass<*>)?.java
            ?: throw KotlinReflectionInternalError("Array parameter type is not a class: $parameterType")
        val componentType =
            if (arrayClass.componentType == KClass::class.java) Class::class.java else arrayClass.componentType
        java.lang.reflect.Array.newInstance(componentType, elements.size).also { array ->
            for ((index, element) in elements.withIndex()) {
                java.lang.reflect.Array.set(array, index, element.toAnnotationArgument(annotationClassName, null, classLoader))
            }
        }
    }
    is KmAnnotationArgument.EnumValue -> {
        val enumClass = classLoader.loadClass(enumClassName.toClassId())
            ?: throw KotlinReflectionInternalError("Unresolved enum class: $enumClassName")
        enumClass.enumConstants.singleOrNull { (it as Enum<*>).name == enumEntryName }
            ?: throw KotlinReflectionInternalError("Unresolved enum entry: $enumClassName.$enumEntryName")
    }
    is KmAnnotationArgument.KClassValue ->
        classLoader.loadClass(className.toClassId())
            ?: throw KotlinReflectionInternalError("Unresolved class: $className")
    is KmAnnotationArgument.LiteralValue<*> -> value
}
