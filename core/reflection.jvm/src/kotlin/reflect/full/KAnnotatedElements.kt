/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("KAnnotatedElements")

package kotlin.reflect.full

import java.lang.annotation.Repeatable
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass

/**
 * Returns an annotation of the given type on this element.
 */
@SinceKotlin("1.1")
inline fun <reified T : Annotation> KAnnotatedElement.findAnnotation(): T? =
    annotations.firstOrNull { it is T } as T?

/**
 * Returns true if this element is annotated with an annotation of type [T].
 */
@SinceKotlin("1.4")
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@WasExperimental(ExperimentalStdlibApi::class)
inline fun <reified T : Annotation> KAnnotatedElement.hasAnnotation(): Boolean =
    findAnnotation<T>() != null

/**
 * Returns all annotations of the given type on this element, including individually applied annotations
 * as well as repeated annotations.
 *
 * In case the annotation is repeated, instances are extracted from the container annotation class similarly to how it happens
 * in Java reflection ([java.lang.reflect.AnnotatedElement.getAnnotationsByType]). This is supported both for Kotlin-repeatable
 * ([kotlin.annotation.Repeatable]) and Java-repeatable ([java.lang.annotation.Repeatable]) annotation classes.
 */
@SinceKotlin("1.7")
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@WasExperimental(ExperimentalStdlibApi::class)
inline fun <reified T : Annotation> KAnnotatedElement.findAnnotations(): List<T> =
    findAnnotations(T::class)

/**
 * Returns all annotations of the given type on this element, including individually applied annotations
 * as well as repeated annotations.
 *
 * In case the annotation is repeated, instances are extracted from the container annotation class similarly to how it happens
 * in Java reflection ([java.lang.reflect.AnnotatedElement.getAnnotationsByType]). This is supported both for Kotlin-repeatable
 * ([kotlin.annotation.Repeatable]) and Java-repeatable ([java.lang.annotation.Repeatable]) annotation classes.
 */
@SinceKotlin("1.7")
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@WasExperimental(ExperimentalStdlibApi::class)
fun <T : Annotation> KAnnotatedElement.findAnnotations(klass: KClass<T>): List<T> {
    val filtered = annotations.filterIsInstance(klass.java)
    if (filtered.isNotEmpty()) return filtered

    val containerClass = klass.java.getAnnotation(Repeatable::class.java)?.value
    if (containerClass != null) {
        val container = annotations.firstOrNull { it.annotationClass.java == containerClass }
        if (container != null) {
            // A repeatable annotation container must have a method "value" returning the array of repeated annotations.
            val valueMethod = container::class.java.getMethod("value")
            @Suppress("UNCHECKED_CAST")
            return (valueMethod(container) as Array<T>).asList()
        }
    }

    return emptyList()
}
