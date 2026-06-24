/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.enclosingMethodOverride
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.util.isAnonymousFunction
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrVisitor

/**
 * Finds enclosing methods for objects inside inline and dynamic lambdas.
 */
internal class RecordEnclosingMethodsLowering(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) =
        irFile.accept(object : IrVisitor<Unit, IrFunction?>() {
            private val classStack = mutableListOf<IrClass>()

            override fun visitClass(declaration: IrClass, data: IrFunction?) {
                classStack.add(declaration)
                super.visitClass(declaration, data)
                classStack.removeLast()
            }

            override fun visitElement(element: IrElement, data: IrFunction?) =
                element.acceptChildren(this, element as? IrFunction ?: data)

            override fun visitCall(expression: IrCall, data: IrFunction?) {
                require(data != null) { "function call not in a method: ${expression.render()}" }
                IndyLambdaMetafactoryLowering.getLambdaMetafactoryIndyImplFunctionRefOrNull(context.symbols, expression)?.let { reference ->
                    val implFunction = reference.symbol.owner
                    // A synthetic forwarder/lambda function has no enclosing method of its own, so record one here.
                    // If the `invokedynamic` was pointed directly at a real function (see IndyLambdaMetafactoryLowering),
                    // that function already has its own enclosing context and must not be overridden.
                    if (implFunction.isSyntheticMetafactoryImplFunction()) {
                        recordEnclosingMethodOverride(implFunction, data)
                    }
                }
                return super.visitCall(expression, data)
            }

            override fun visitRichFunctionReference(expression: IrRichFunctionReference, data: IrFunction?) {
                require(data != null) { "inline lambda not in a method: ${expression.render()}" }
                recordEnclosingMethodOverride(expression.invokeFunction, data)
                return super.visitRichFunctionReference(expression, data)
            }

            private fun recordEnclosingMethodOverride(from: IrFunction, to: IrFunction) {
                val old = from.enclosingMethodOverride
                if (old != null) {
                    // A single lambda can be referenced multiple times if it is in a field initializer
                    // or an anonymous initializer block and there are multiple non-delegating constructors.
                    classStack.last().primaryConstructor?.let {
                        from.enclosingMethodOverride = it
                    }
                } else {
                    from.enclosingMethodOverride = to
                }
            }
        }, null)
}

/**
 * Whether this is a synthetic implementation function generated for an `invokedynamic`/`LambdaMetafactory` closure
 * (a lambda body or a function-reference forwarder), as opposed to a real function the `invokedynamic` was pointed at
 * directly. Mirrors `LambdaMetafactoryArgumentsBuilder.isAdaptable`.
 */
private fun IrFunction.isSyntheticMetafactoryImplFunction(): Boolean =
    when (origin) {
        IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA,
        JvmLoweredDeclarationOrigin.PROXY_FUN_FOR_METAFACTORY,
        JvmLoweredDeclarationOrigin.SYNTHETIC_PROXY_FUN_FOR_METAFACTORY,
            -> true
        IrDeclarationOrigin.LOCAL_FUNCTION -> isAnonymousFunction
        else -> false
    }
