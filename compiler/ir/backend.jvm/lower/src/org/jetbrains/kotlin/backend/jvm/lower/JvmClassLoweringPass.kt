/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.StageController
import org.jetbrains.kotlin.ir.declarations.impl.AbstractIrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name

interface JvmClassLoweringPass : FileLoweringPass {
    val context: JvmBackendContext

    override fun lower(irFile: IrFile) {
        val size = context.irClasses.size
        for (i in 0 until size) { // Prevent ConcurrentModificationException
            lower(context.irClasses[i])
        }
    }

    fun lower(irClass: IrClass)
}


internal val startTrackingClasses = makeIrFilePhase(
    ::StartTrackingClasses,
    name = "StartTrackingClasses",
    description = "StartTrackingClasses"
)

class StartTrackingClasses(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        (context.irFactory as JvmIrFactoryImpl).trackCreatedClasses = true
        irFile.acceptVoid(CollectClassesVisitor(context))
    }
}


internal val stopTrackingClasses = makeIrFilePhase(
    ::StopTrackingClasses,
    name = "StopTrackingClasses",
    description = "StopTrackingClasses"
)

class StopTrackingClasses(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        (context.irFactory as JvmIrFactoryImpl).trackCreatedClasses = false
        context.irClasses.clear()
    }
}


private class CollectClassesVisitor(
    private val context: JvmBackendContext
) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        declaration.acceptChildrenVoid(this)
        context.irClasses.add(declaration)
    }
}

class JvmIrFactoryImpl : AbstractIrFactoryImpl() {
    lateinit var context: JvmBackendContext

    override var trackCreatedClasses: Boolean = false

    override val stageController: StageController = StageController()

    override fun createClass(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        visibility: DescriptorVisibility,
        symbol: IrClassSymbol,
        kind: ClassKind,
        modality: Modality,
        isExternal: Boolean,
        isCompanion: Boolean,
        isInner: Boolean,
        isData: Boolean,
        isValue: Boolean,
        isExpect: Boolean,
        isFun: Boolean,
        hasEnumEntries: Boolean,
        source: SourceElement,
    ): IrClass {
        return super.createClass(
            startOffset,
            endOffset,
            origin,
            name,
            visibility,
            symbol,
            kind,
            modality,
            isExternal,
            isCompanion,
            isInner,
            isData,
            isValue,
            isExpect,
            isFun,
            hasEnumEntries,
            source
        ).also {
            if (trackCreatedClasses) {
                context.irClasses += it
            }
        }
    }
}
