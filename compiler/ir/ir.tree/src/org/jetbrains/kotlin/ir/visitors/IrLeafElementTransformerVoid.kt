/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*

abstract class IrLeafElementTransformerVoid : IrElementTransformerVoid() {
    override fun visitElement(element: IrElement): IrElement = element.transformChildren()

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment = declaration.transformChildren()

    override fun visitPackageFragment(declaration: IrPackageFragment): IrPackageFragment = declaration.transformChildren()

    override fun visitFile(declaration: IrFile): IrFile = declaration.transformChildren()

    override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment): IrExternalPackageFragment =
        declaration.transformChildren()

    override fun visitDeclaration(declaration: IrDeclaration): IrStatement = declaration.transformChildren()

    override fun visitScript(declaration: IrScript): IrStatement = declaration.transformChildren()

    override fun visitClass(declaration: IrClass): IrStatement = declaration.transformChildren()

    override fun visitFunction(declaration: IrFunction): IrStatement = declaration.transformChildren()

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement = declaration.transformChildren()

    override fun visitConstructor(declaration: IrConstructor): IrStatement = declaration.transformChildren()

    override fun visitProperty(declaration: IrProperty): IrStatement = declaration.transformChildren()

    override fun visitField(declaration: IrField): IrStatement = declaration.transformChildren()

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrStatement = declaration.transformChildren()

    override fun visitEnumEntry(declaration: IrEnumEntry): IrStatement = declaration.transformChildren()

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer): IrStatement = declaration.transformChildren()

    override fun visitTypeParameter(declaration: IrTypeParameter): IrStatement = declaration.transformChildren()

    override fun visitValueParameter(declaration: IrValueParameter): IrStatement = declaration.transformChildren()

    override fun visitVariable(declaration: IrVariable): IrStatement = declaration.transformChildren()

    override fun visitTypeAlias(declaration: IrTypeAlias): IrStatement = declaration.transformChildren()

    override fun visitBody(body: IrBody): IrBody = body.transformChildren()

    override fun visitExpressionBody(body: IrExpressionBody): IrBody = body.transformChildren()

    override fun visitBlockBody(body: IrBlockBody): IrBody = body.transformChildren()

    override fun visitSyntheticBody(body: IrSyntheticBody): IrBody = body.transformChildren()

    override fun visitSuspendableExpression(expression: IrSuspendableExpression): IrExpression = expression.transformChildren()

    override fun visitSuspensionPoint(expression: IrSuspensionPoint): IrExpression = expression.transformChildren()

    override fun visitExpression(expression: IrExpression): IrExpression = expression.transformChildren()

    override fun <T> visitConst(expression: IrConst<T>): IrExpression = expression.transformChildren()

    override fun visitVararg(expression: IrVararg): IrExpression = expression.transformChildren()

    override fun visitSpreadElement(spread: IrSpreadElement): IrSpreadElement = spread.transformChildren()

    override fun visitContainerExpression(expression: IrContainerExpression): IrExpression = expression.transformChildren()

    override fun visitBlock(expression: IrBlock): IrExpression = expression.transformChildren()

    override fun visitComposite(expression: IrComposite): IrExpression = expression.transformChildren()

    override fun visitStringConcatenation(expression: IrStringConcatenation): IrExpression = expression.transformChildren()

    override fun visitDeclarationReference(expression: IrDeclarationReference): IrExpression = expression.transformChildren()

    override fun visitSingletonReference(expression: IrGetSingletonValue): IrExpression = expression.transformChildren()

    override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression = expression.transformChildren()

    override fun visitGetEnumValue(expression: IrGetEnumValue): IrExpression = expression.transformChildren()

    override fun visitValueAccess(expression: IrValueAccessExpression): IrExpression = expression.transformChildren()

    override fun visitGetValue(expression: IrGetValue): IrExpression = expression.transformChildren()

    override fun visitSetVariable(expression: IrSetVariable): IrExpression = expression.transformChildren()

    override fun visitFieldAccess(expression: IrFieldAccessExpression): IrExpression = expression.transformChildren()

    override fun visitGetField(expression: IrGetField): IrExpression = expression.transformChildren()

    override fun visitSetField(expression: IrSetField): IrExpression = expression.transformChildren()

    override fun visitMemberAccess(expression: IrMemberAccessExpression): IrExpression = expression.transformChildren()

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression = expression.transformChildren()

    override fun visitCall(expression: IrCall): IrExpression = expression.transformChildren()

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression = expression.transformChildren()

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression = expression.transformChildren()

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall): IrExpression = expression.transformChildren()

    override fun visitGetClass(expression: IrGetClass): IrExpression = expression.transformChildren()

    override fun visitCallableReference(expression: IrCallableReference): IrExpression = expression.transformChildren()

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression = expression.transformChildren()

    override fun visitPropertyReference(expression: IrPropertyReference): IrExpression = expression.transformChildren()

    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference): IrExpression =
        expression.transformChildren()

    override fun visitFunctionExpression(expression: IrFunctionExpression): IrExpression = expression.transformChildren()

    override fun visitClassReference(expression: IrClassReference): IrExpression = expression.transformChildren()

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall): IrExpression = expression.transformChildren()

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression = expression.transformChildren()

    override fun visitWhen(expression: IrWhen): IrExpression = expression.transformChildren()

    override fun visitBranch(branch: IrBranch): IrBranch = branch.transformChildren()

    override fun visitElseBranch(branch: IrElseBranch): IrElseBranch = branch.transformChildren()

    override fun visitLoop(loop: IrLoop): IrExpression = loop.transformChildren()

    override fun visitWhileLoop(loop: IrWhileLoop): IrExpression = loop.transformChildren()

    override fun visitDoWhileLoop(loop: IrDoWhileLoop): IrExpression = loop.transformChildren()

    override fun visitTry(aTry: IrTry): IrExpression = aTry.transformChildren()

    override fun visitCatch(aCatch: IrCatch): IrCatch = aCatch.transformChildren()

    override fun visitBreakContinue(jump: IrBreakContinue): IrExpression = jump.transformChildren()

    override fun visitBreak(jump: IrBreak): IrExpression = jump.transformChildren()

    override fun visitContinue(jump: IrContinue): IrExpression = jump.transformChildren()

    override fun visitReturn(expression: IrReturn): IrExpression = expression.transformChildren()

    override fun visitThrow(expression: IrThrow): IrExpression = expression.transformChildren()

    override fun visitDynamicExpression(expression: IrDynamicExpression): IrExpression = expression.transformChildren()

    override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression): IrExpression = expression.transformChildren()

    override fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression): IrExpression = expression.transformChildren()

    override fun visitErrorDeclaration(declaration: IrErrorDeclaration): IrStatement = declaration.transformChildren()

    override fun visitErrorExpression(expression: IrErrorExpression): IrExpression = expression.transformChildren()

    override fun visitErrorCallExpression(expression: IrErrorCallExpression): IrExpression = expression.transformChildren()
}
