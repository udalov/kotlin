/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.lower.InitializersLowering
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols

class JvmInitializersLowering(context: JvmBackendContext) : InitializersLowering(context) {
    private val backendContext: JvmBackendContext get() = context as JvmBackendContext

    override fun copyBlock(block: IrBlock, constructor: IrConstructor): IrBlock =
        block.deepCopyWithSymbols(constructor) { symbolRemapper, typeRemapper ->
            object : DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper) {
                override fun visitFunctionReference(expression: IrFunctionReference): IrFunctionReference =
                    super.visitFunctionReference(expression).apply {
                        backendContext.copyCallableReference(expression, this)
                    }
            }
        }
}
