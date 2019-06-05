/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.structure.impl

import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.load.java.structure.JavaMethodBase
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.load.java.structure.JavaValueParameter

abstract class JavaMethodBaseImpl(psiMethod: PsiMethod) : JavaMemberImpl<PsiMethod>(psiMethod), JavaMethodBase {
    override val valueParameters: List<JavaValueParameter>
        get() = valueParameters(psi.parameterList.parameters)

    override val typeParameters: List<JavaTypeParameter>
        get() = typeParameters(psi.typeParameters)
}
