/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.ClassCodegen
import org.jetbrains.kotlin.backend.jvm.mapping.mapClass
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

abstract class IntrinsicFunction(val argsTypes: List<Type>) {
    abstract fun genInvokeInstruction(v: InstructionAdapter)

    companion object {
        fun create(
            expression: IrFunctionAccessExpression,
            classCodegen: ClassCodegen,
            invokeInstruction: IntrinsicFunction.(InstructionAdapter) -> Unit,
        ): IntrinsicFunction = create(expression.argTypes(classCodegen), invokeInstruction)

        fun create(
            argsTypes: List<Type>,
            invokeInstruction: IntrinsicFunction.(InstructionAdapter) -> Unit,
        ): IntrinsicFunction =
            object : IntrinsicFunction(argsTypes) {
                override fun genInvokeInstruction(v: InstructionAdapter) = invokeInstruction(v)
            }
    }
}

internal fun IrFunctionAccessExpression.argTypes(classCodegen: ClassCodegen): List<Type> {
    val callee = symbol.owner
    val signature = classCodegen.methodSignatureMapper.mapSignatureSkipGeneric(callee)
    return arrayListOf<Type>().apply {
        if (dispatchReceiver != null) {
            add(classCodegen.typeMapper.mapClass(callee.parentAsClass))
        }
        addAll(signature.asmMethod.argumentTypes)
    }
}
