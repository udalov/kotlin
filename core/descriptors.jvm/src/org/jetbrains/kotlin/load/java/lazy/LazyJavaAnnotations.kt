/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.java.lazy

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.components.JavaAnnotationMapper
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaAnnotationDescriptor
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaAnnotationOwner
import org.jetbrains.kotlin.name.FqName

class LazyJavaAnnotations(
    private val c: LazyJavaResolverContext,
    private val annotationOwner: JavaAnnotationOwner,
    private val areAnnotationsFreshlySupported: Boolean = false
) : Annotations {
    private val annotationDescriptors = c.components.storageManager.createMemoizedFunctionWithNullableValues { annotation: JavaAnnotation ->
        JavaAnnotationMapper.mapOrResolveJavaAnnotation(annotation, c, areAnnotationsFreshlySupported)
    }

    override fun findAnnotation(fqName: FqName): AnnotationDescriptor? =
        annotationOwner.findAnnotation(fqName)?.let(annotationDescriptors)
            ?: JavaAnnotationMapper.findMappedJavaAnnotation(fqName, annotationOwner, c)
            ?: if (fqName == JvmAnnotationNames.REPEATABLE_ANNOTATION) findJavaRepeatableAnnotation() else null

    override fun iterator(): Iterator<AnnotationDescriptor> =
        (annotationOwner.annotations.asSequence().map(annotationDescriptors) +
                JavaAnnotationMapper.findMappedJavaAnnotation(StandardNames.FqNames.deprecated, annotationOwner, c) +
                findJavaRepeatableAnnotation()
                ).filterNotNull().iterator()

    private fun findJavaRepeatableAnnotation(): AnnotationDescriptor? =
        annotationOwner.findAnnotation(JvmAnnotationNames.REPEATABLE_ANNOTATION)?.let {
            LazyJavaAnnotationDescriptor(c, it)
        }

    override fun isEmpty(): Boolean =
        annotationOwner.annotations.isEmpty() && !annotationOwner.isDeprecatedInJavaDoc
}

fun LazyJavaResolverContext.resolveAnnotations(annotationsOwner: JavaAnnotationOwner): Annotations =
    LazyJavaAnnotations(this, annotationsOwner)
