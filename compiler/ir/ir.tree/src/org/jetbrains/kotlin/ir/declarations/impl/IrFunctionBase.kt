/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name

abstract class IrFunctionBase(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val name: Name,
    override var visibility: Visibility,
    override val isInline: Boolean,
    override val isExternal: Boolean,
    override val isExpect: Boolean,
    returnType: IrType
) :
    IrDeclarationBase(startOffset, endOffset, origin),
    IrFunction {

    @Suppress("DEPRECATION")
    final override var returnType: IrType = returnType
        get() = if (field === org.jetbrains.kotlin.ir.types.impl.IrUninitializedType) {
            error("Return type is not initialized")
        } else {
            field
        }

    override var typeParameters: List<IrTypeParameter> = emptyList()

    private var dispatchReceiverParameterField: IrValueParameterImpl? = null
    override var dispatchReceiverParameter: IrValueParameter?
        get() = dispatchReceiverParameterField
        set(value) { dispatchReceiverParameterField = value as IrValueParameterImpl? }
    private var extensionReceiverParameterField: IrValueParameterImpl? = null
    override var extensionReceiverParameter: IrValueParameter?
        get() = extensionReceiverParameterField
        set(value) { extensionReceiverParameterField = value as IrValueParameterImpl? }
    override var valueParameters: List<IrValueParameter> = emptyList()

    private var bodyField: IrElementBase? = null

    final override var body: IrBody?
        get() = bodyField as IrBody?
        set(value) { bodyField = value as IrElementBase? }

    override var metadata: MetadataSource? = null

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        val tp = typeParameters.iterator()
        while (tp.hasNext()) {
            (tp.next() as IrElementBase?)!!.accept(visitor, data)
        }

        dispatchReceiverParameterField?.accept(visitor, data)
        extensionReceiverParameterField?.accept(visitor, data)

        val vp = valueParameters.iterator()
        while (vp.hasNext()) {
            (vp.next() as IrElementBase?)!!.accept(visitor, data)
        }

        bodyField?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        typeParameters = typeParameters.mapOptimized0(transformer, data)

        dispatchReceiverParameterField = uncheckedCast(dispatchReceiverParameterField?.transform(transformer, data))
        extensionReceiverParameterField = uncheckedCast(extensionReceiverParameterField?.transform(transformer, data))
        valueParameters = valueParameters.mapOptimized0(transformer, data)

        bodyField = uncheckedCast(bodyField?.transform(transformer, data))
    }
}

internal fun <T, D> List<T>.mapOptimized0(transformer: IrElementTransformer<D>, data: D): List<T> {
    var result: ArrayList<T>? = null
    for (i in indices) {
        val item = this[i]
        val transformed = uncheckedCast<T>((item as IrElementBase?)!!.transform(transformer, data))
        if (transformed !== item && result == null) {
            result = ArrayList(this)
        }
        result?.set(i, transformed)
    }
    return result ?: this
}