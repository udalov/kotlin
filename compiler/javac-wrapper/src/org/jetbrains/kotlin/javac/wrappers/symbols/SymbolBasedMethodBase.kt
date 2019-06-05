/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.javac.wrappers.symbols

import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaMethodBase
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.load.java.structure.JavaValueParameter
import org.jetbrains.kotlin.name.ClassId
import javax.lang.model.element.ExecutableElement

abstract class SymbolBasedMethodBase(
    element: ExecutableElement,
    containingClass: JavaClass,
    javac: JavacWrapper
) : SymbolBasedMember<ExecutableElement>(element, containingClass, javac), JavaMethodBase {
    override val typeParameters: List<JavaTypeParameter>
        get() = element.typeParameters.map { SymbolBasedTypeParameter(it, javac) }

    override val valueParameters: List<JavaValueParameter>
        get() = element.valueParameters(javac)

    override val thrownExceptions: List<ClassId>
        get() = emptyList() // TODO
}
