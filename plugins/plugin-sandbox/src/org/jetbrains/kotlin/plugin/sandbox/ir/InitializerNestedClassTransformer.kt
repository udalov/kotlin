/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.types.isNullableString
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.plugin.sandbox.fir.fqn

/**
 * For a class annotated with `@GenerateInitializerNestedClass`, generates a nested class `Initializer` with a single `init` function,
 * that takes an instance of the class and sets all mutable String-typed member properties to their name.
 */
class InitializerNestedClassTransformer(val context: IrPluginContext) : IrVisitorVoid() {
    companion object {
        private val ANNOTATION_FQN = "GenerateInitializerNestedClass".fqn()
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        if (declaration.annotations.hasAnnotation(ANNOTATION_FQN)) {
            declaration.declarations += context.irFactory.buildClass {
                startOffset = UNDEFINED_OFFSET
                endOffset = UNDEFINED_OFFSET
                name = Name.identifier("Initializer")
            }.apply Initializer@{
                parent = declaration
                createThisReceiverParameter()
                declarations += context.irFactory.buildFun {
                    name = Name.identifier("init")
                    returnType = context.irBuiltIns.unitType
                }.apply {
                    parent = this@Initializer
                    val instance = addValueParameter("instance", declaration.typeWith())
                    body = context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
                        for (property in declaration.properties) {
                            if (!property.getter!!.returnType.isNullableString()) continue
                            val setter = property.setter ?: continue
                            statements += IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.unitType, setter.symbol).apply {
                                dispatchReceiver = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, instance.symbol)
                                arguments[setter.parameters.last()] = property.name.asString().toIrConst(context.irBuiltIns.stringType)
                            }
                        }
                    }
                }
            }
        }
        visitElement(declaration)
    }
}
