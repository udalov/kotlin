/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.javac.wrappers.trees

import com.sun.source.tree.CompilationUnitTree
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaMethodBase
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.load.java.structure.JavaValueParameter
import org.jetbrains.kotlin.name.Name

abstract class TreeBasedMethodBase(
    tree: JCTree.JCMethodDecl,
    compilationUnit: CompilationUnitTree,
    containingClass: JavaClass,
    javac: JavacWrapper
) : TreeBasedMember<JCTree.JCMethodDecl>(tree, compilationUnit, containingClass, javac), JavaMethodBase {
    override val name: Name
        get() = Name.identifier(tree.name.toString())

    override val typeParameters: List<JavaTypeParameter>
        get() = tree.typeParameters.map { TreeBasedTypeParameter(it, compilationUnit, javac, this) }

    override val valueParameters: List<JavaValueParameter>
        get() = tree.parameters.map { TreeBasedValueParameter(it, compilationUnit, javac, this) }
}
