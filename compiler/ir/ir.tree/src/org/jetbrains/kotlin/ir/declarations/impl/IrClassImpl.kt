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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.mapOptimized
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal

class IrClassImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrClassSymbol,
    override val name: Name,
    override val kind: ClassKind,
    override var visibility: Visibility,
    override var modality: Modality,
    override val isCompanion: Boolean = false,
    override val isInner: Boolean = false,
    override val isData: Boolean = false,
    override val isExternal: Boolean = false,
    override val isInline: Boolean = false,
    override val isExpect: Boolean = false,
    override val isFun: Boolean = false,
    override val source: SourceElement = SourceElement.NO_SOURCE
) :
    IrDeclarationBase(startOffset, endOffset, origin),
    IrClass {

    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrClassSymbol,
        descriptor: ClassDescriptor,
        name: Name = descriptor.name,
        visibility: Visibility = descriptor.visibility,
        modality: Modality = descriptor.modality
    ) : this(
        startOffset, endOffset, origin, symbol,
        name, kind = descriptor.kind,
        visibility = visibility, modality = modality,
        isCompanion = descriptor.isCompanionObject, isInner = descriptor.isInner,
        isData = descriptor.isData, isExternal = descriptor.isEffectivelyExternal(),
        isInline = descriptor.isInline, isExpect = descriptor.isExpect,
        isFun = descriptor.isFun,
        source = descriptor.source
    )

    init {
        symbol.bind(this)
    }

    @ObsoleteDescriptorBasedAPI
    override val descriptor: ClassDescriptor get() = symbol.descriptor

    override var thisReceiver: IrValueParameter? = null

    override val declarations: MutableList<IrDeclaration> = ArrayList()

    override var typeParameters: List<IrTypeParameter> = emptyList()

    override var superTypes: List<IrType> = emptyList()

    override var metadata: MetadataSource? = null

    override var attributeOwnerId: IrAttributeContainer = this

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitClass(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        thisReceiver?.accept(visitor, data)
        typeParameters.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        thisReceiver = thisReceiver?.transform(transformer, data)
        typeParameters = typeParameters.mapOptimized { it.transform(transformer, data) }
        declarations.transformDeclarationsInPlace(transformer, data)
    }
}

internal fun <T : IrElement, D> MutableList<T>.transformDeclarationsInPlace(transformer: IrElementTransformer<D>, data: D) {
    val iterator = this.listIterator()
    while (iterator.hasNext()) {
        val next = iterator.next() as IrDeclarationBase?
        iterator.set(uncheckedCast(next!!.transform(transformer, data)))
    }
}

@Suppress("UNCHECKED_CAST")
internal fun <T> uncheckedCast(value: Any?): T =
    value as T
