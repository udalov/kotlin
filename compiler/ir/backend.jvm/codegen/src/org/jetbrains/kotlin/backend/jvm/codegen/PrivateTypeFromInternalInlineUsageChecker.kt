/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmBackendErrors
import org.jetbrains.kotlin.backend.jvm.ir.fileParent
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.MethodNode

internal class PrivateTypeFromInternalInlineUsageChecker(
    private val context: JvmBackendContext,
) : MethodVisitor(Opcodes.API_VERSION) {
    private val result: MutableSet<ClassId> = mutableSetOf()
    private fun findPrivateClassUsages(node: MethodNode): Collection<ClassId> = result.also { node.accept(this) }

    override fun visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean) {
        // TODO: won't work with "$" in the name.
        for (classDescriptor in context.state.jvmBackendClassResolver.resolveToClassDescriptors(Type.getObjectType(owner))) {
            // TODO: test on private-to-this
            // TODO: or effectively-private?
            if (DescriptorVisibilities.isPrivate(classDescriptor.visibility)) {
                // TODO: check the string "<no name provided>" in a test
                result.add(classDescriptor.classId ?: ClassId(FqName.ROOT, SpecialNames.NO_NAME_PROVIDED))
            }
        }
    }

    companion object {
        fun check(caller: IrFunction, call: IrExpression, callee: IrFunction, node: MethodNode, context: JvmBackendContext) {
            if (caller.visibility == DescriptorVisibilities.INTERNAL && callee.isEffectivelyPrivate()) {
                val privateClassIds = PrivateTypeFromInternalInlineUsageChecker(context).findPrivateClassUsages(node)
                for (classId in privateClassIds) {
                    context.ktDiagnosticReporter.at(call, caller.fileParent)
                        .report(JvmBackendErrors.PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION, classId)
                }
            }
        }
    }
}
