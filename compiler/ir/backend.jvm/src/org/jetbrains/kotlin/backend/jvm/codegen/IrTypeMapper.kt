/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.CallableMethod
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

class IrTypeMapper(val kotlinTypeMapper: KotlinTypeMapper) {
    val classBuilderMode: ClassBuilderMode
        get() = kotlinTypeMapper.classBuilderMode

    fun classInternalName(irClass: IrClass): String =
        kotlinTypeMapper.classInternalName(irClass.descriptor)

    fun mapAsmMethod(irFunction: IrFunction): Method =
        kotlinTypeMapper.mapAsmMethod(irFunction.descriptor)

    fun mapClass(irClass: IrClass): Type =
        kotlinTypeMapper.mapClass(irClass.descriptor)

    fun mapFieldSignature(irType: IrType, irField: IrField): String? =
        kotlinTypeMapper.mapFieldSignature(irType.toKotlinType(), irField.descriptor)

    fun mapFunctionName(irReturnTarget: IrReturnTarget, ownerKind: OwnerKind): String =
        kotlinTypeMapper.mapFunctionName(irReturnTarget.descriptor, ownerKind)

    fun mapImplementationOwner(irDeclaration: IrDeclaration): Type =
        kotlinTypeMapper.mapImplementationOwner(irDeclaration.descriptor)

    fun mapReturnType(irReturnTarget: IrReturnTarget): Type =
        kotlinTypeMapper.mapReturnType(irReturnTarget.descriptor)

    fun mapSignatureSkipGeneric(f: IrFunction, kind: OwnerKind = OwnerKind.IMPLEMENTATION): JvmMethodSignature =
        kotlinTypeMapper.mapSignatureSkipGeneric(f.descriptor, kind)

    fun mapSignatureWithGeneric(f: IrFunction, kind: OwnerKind): JvmMethodGenericSignature =
        kotlinTypeMapper.mapSignatureWithGeneric(f.descriptor, kind)

    fun mapSupertype(irType: IrType, sw: JvmSignatureWriter): Type =
        kotlinTypeMapper.mapSupertype(irType.toKotlinType(), sw)

    fun mapToCallableMethod(
        f: IrFunction, superCall: Boolean, kind: OwnerKind? = null, resolvedCall: ResolvedCall<*>? = null
    ): CallableMethod = kotlinTypeMapper.mapToCallableMethod(f.descriptor, superCall, kind, resolvedCall)

    fun mapType(irType: IrType): Type =
        kotlinTypeMapper.mapType(irType.toKotlinType())

    fun mapType(irClass: IrClass): Type =
        kotlinTypeMapper.mapType(irClass.descriptor)

    fun mapType(irField: IrField): Type =
        kotlinTypeMapper.mapType(irField.descriptor)

    fun mapType(irValueParameter: IrValueParameter): Type =
        kotlinTypeMapper.mapType(irValueParameter.descriptor)

    fun mapType(irVariable: IrVariable): Type =
        kotlinTypeMapper.mapType(irVariable.descriptor)

    fun mapType(irType: IrType, sw: JvmSignatureWriter, mode: TypeMappingMode): Type =
        kotlinTypeMapper.mapType(irType.toKotlinType(), sw, mode)

    fun mapTypeAsDeclaration(irType: IrType): Type =
        kotlinTypeMapper.mapTypeAsDeclaration(irType.toKotlinType())

    fun mapTypeParameter(irType: IrType, signatureWriter: JvmSignatureWriter): Type =
        kotlinTypeMapper.mapTypeParameter(irType.toKotlinType(), signatureWriter)

    fun writeFormalTypeParameters(irParameters: List<IrTypeParameter>, sw: JvmSignatureWriter) {
        kotlinTypeMapper.writeFormalTypeParameters(irParameters.map { it.descriptor }, sw)
    }

    fun boxType(irType: IrType): Type =
        AsmUtil.boxType(mapType(irType), irType.toKotlinType(), kotlinTypeMapper)
}
