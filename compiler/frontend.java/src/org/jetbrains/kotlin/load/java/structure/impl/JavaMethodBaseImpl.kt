/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.structure.impl

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.load.java.structure.JavaMethodBase
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.load.java.structure.JavaValueParameter
import org.jetbrains.kotlin.load.java.structure.classId
import org.jetbrains.kotlin.name.ClassId

abstract class JavaMethodBaseImpl(psiMethod: PsiMethod) : JavaMemberImpl<PsiMethod>(psiMethod), JavaMethodBase {
    override val valueParameters: List<JavaValueParameter>
        get() = valueParameters(psi.parameterList.parameters)

    override val typeParameters: List<JavaTypeParameter>
        get() = typeParameters(psi.typeParameters)

    override val thrownExceptions: List<ClassId>
        get() = psi.throwsTypes.mapNotNull { throwsType ->
            val psiClass = throwsType.resolve() as? PsiClass
            // We perform JVM erasure because it's not possible to represent `throws E` (where E is a type parameter) in Kotlin.
            // TODO: this will lead to problems when using delegation to Java members that use this feature
            val erasure = when (val classifier = psiClass?.let { JavaClassifierImpl.create(it) }) {
                is JavaClassImpl -> classifier
                is JavaTypeParameterImpl -> classifier.erasure
                else -> null
            }
            erasure?.classId
        }

    private val JavaTypeParameterImpl.erasure: JavaClassImpl?
        get() {
            // Note that Throwable is a class, therefore there can be either one class bound and zero or more interface bounds,
            // or exactly one type parameter bound.
            for (bound in upperBounds) {
                val klass = bound.classifier
                if (klass is JavaClassImpl && !klass.isInterface) return klass
            }
            return (upperBounds.singleOrNull()?.classifier as? JavaTypeParameterImpl)?.erasure
        }
}
