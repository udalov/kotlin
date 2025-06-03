/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.*
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.substitute
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Type

abstract class CallBasedIntrinsicMethod : IntrinsicMethod() {
    abstract fun toCallable(
        expression: IrFunctionAccessExpression, signature: JvmMethodSignature, classCodegen: ClassCodegen,
    ): IntrinsicFunction

    protected open fun changeReturnType(type: Type): Type = type

    final override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue? {
        val signature = codegen.methodSignatureMapper.mapSignatureSkipGeneric(expression.symbol.owner)
        val callable = toCallable(expression, signature, codegen.classCodegen)
        generateArguments(expression, codegen, data, callable.argsTypes)
        with(codegen) { expression.markLineNumber(startOffset = true) }
        callable.genInvokeInstruction(codegen.mv)
        return MaterialValue(codegen, changeReturnType(signature.returnType), expression.type)
    }

    private fun generateArguments(
        expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo, argsTypes: List<Type>,
    ) {
        for ([parameter, argument] in expression.symbol.owner.parameters zip expression.arguments) {
            if (argument != null) {
                codegen.gen(argument, argsTypes[parameter.indexInParameters], argument.type, data)
            } else if (parameter.isVararg) {
                // TODO: is there an easier way to get the substituted type of an empty vararg argument?
                val arrayType = codegen.typeMapper.mapType(
                    parameter.type.substitute(expression.symbol.owner.typeParameters, expression.typeArguments.map { it!! })
                )
                codegen.mv.aconst(0)
                codegen.mv.newarray(AsmUtil.correctElementType(arrayType))
            } else {
                error("Unknown parameter ${parameter.name} in: ${expression.dump()}")
            }
        }
    }
}
