/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.psi2ir.PsiSourceManager
import org.jetbrains.kotlin.resolve.BindingContext

class MetadataInfo(
    module: IrModuleFragment,
    sourceManager: PsiSourceManager,
    bindingContext: BindingContext
) {
    private val classes = mutableMapOf<IrAttributeContainer, ClassDescriptor>()
    private val files = mutableMapOf<IrAttributeContainer, List<DeclarationDescriptor>>()

    init {
        module.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                classes[declaration] = declaration.descriptor
                super.visitClass(declaration)
            }

            override fun visitFile(declaration: IrFile) {
                files[declaration] = declaration.getMemberDescriptors(sourceManager, bindingContext)
                super.visitFile(declaration)
            }
        })
    }

    fun getClassMetadata(irClass: IrClass): ClassDescriptor? =
        classes[irClass.attributeOwnerId]

    fun getFileMetadata(irClass: IrClass): List<DeclarationDescriptor>? =
        files[irClass.attributeOwnerId]
}

internal fun IrFile.getMemberDescriptors(sourceManager: PsiSourceManager, bindingContext: BindingContext): List<DeclarationDescriptor> {
    val ktFile = sourceManager.getKtFile(this)!!
    return CodegenUtil.getMemberDescriptorsToGenerate(ktFile, bindingContext)
}
