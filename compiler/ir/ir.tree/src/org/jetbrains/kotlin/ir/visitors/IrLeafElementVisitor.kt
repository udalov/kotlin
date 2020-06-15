/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*

abstract class IrLeafElementVisitor<out R, in D> : IrElementVisitor<R, D> {
    abstract override fun visitElement(element: IrElement, data: D): R

    override fun visitModuleFragment(declaration: IrModuleFragment, data: D): R = visitElement(declaration, data)

    override fun visitPackageFragment(declaration: IrPackageFragment, data: D): R = visitElement(declaration, data)

    override fun visitFile(declaration: IrFile, data: D): R = visitElement(declaration, data)

    override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment, data: D): R = visitElement(declaration, data)

    override fun visitScript(declaration: IrScript, data: D): R = visitElement(declaration, data)

    override fun visitDeclaration(declaration: IrDeclaration, data: D): R = visitElement(declaration, data)

    override fun visitClass(declaration: IrClass, data: D): R = visitElement(declaration, data)

    override fun visitFunction(declaration: IrFunction, data: D): R = visitElement(declaration, data)

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: D): R = visitElement(declaration, data)

    override fun visitConstructor(declaration: IrConstructor, data: D): R = visitElement(declaration, data)

    override fun visitProperty(declaration: IrProperty, data: D): R = visitElement(declaration, data)

    override fun visitField(declaration: IrField, data: D): R = visitElement(declaration, data)

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: D): R = visitElement(declaration, data)

    override fun visitVariable(declaration: IrVariable, data: D): R = visitElement(declaration, data)

    override fun visitEnumEntry(declaration: IrEnumEntry, data: D): R = visitElement(declaration, data)

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: D): R = visitElement(declaration, data)

    override fun visitTypeParameter(declaration: IrTypeParameter, data: D): R = visitElement(declaration, data)

    override fun visitValueParameter(declaration: IrValueParameter, data: D): R = visitElement(declaration, data)

    override fun visitTypeAlias(declaration: IrTypeAlias, data: D): R = visitElement(declaration, data)

    override fun visitBody(body: IrBody, data: D): R = visitElement(body, data)

    override fun visitExpressionBody(body: IrExpressionBody, data: D): R = visitElement(body, data)

    override fun visitBlockBody(body: IrBlockBody, data: D): R = visitElement(body, data)

    override fun visitSyntheticBody(body: IrSyntheticBody, data: D): R = visitElement(body, data)

    override fun visitSuspendableExpression(expression: IrSuspendableExpression, data: D): R = visitElement(expression, data)

    override fun visitSuspensionPoint(expression: IrSuspensionPoint, data: D): R = visitElement(expression, data)

    override fun visitExpression(expression: IrExpression, data: D): R = visitElement(expression, data)

    override fun <T> visitConst(expression: IrConst<T>, data: D): R = visitElement(expression, data)

    override fun visitVararg(expression: IrVararg, data: D): R = visitElement(expression, data)

    override fun visitSpreadElement(spread: IrSpreadElement, data: D): R = visitElement(spread, data)

    override fun visitContainerExpression(expression: IrContainerExpression, data: D): R = visitElement(expression, data)

    override fun visitBlock(expression: IrBlock, data: D): R = visitElement(expression, data)

    override fun visitComposite(expression: IrComposite, data: D): R = visitElement(expression, data)

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: D): R = visitElement(expression, data)

    override fun visitDeclarationReference(expression: IrDeclarationReference, data: D): R = visitElement(expression, data)

    override fun visitSingletonReference(expression: IrGetSingletonValue, data: D): R = visitElement(expression, data)

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: D): R = visitElement(expression, data)

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: D): R = visitElement(expression, data)

    override fun visitValueAccess(expression: IrValueAccessExpression, data: D): R = visitElement(expression, data)

    override fun visitGetValue(expression: IrGetValue, data: D): R = visitElement(expression, data)

    override fun visitSetVariable(expression: IrSetVariable, data: D): R = visitElement(expression, data)

    override fun visitFieldAccess(expression: IrFieldAccessExpression, data: D): R = visitElement(expression, data)

    override fun visitGetField(expression: IrGetField, data: D): R = visitElement(expression, data)

    override fun visitSetField(expression: IrSetField, data: D): R = visitElement(expression, data)

    override fun visitMemberAccess(expression: IrMemberAccessExpression, data: D): R = visitElement(expression, data)

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: D): R = visitElement(expression, data)

    override fun visitCall(expression: IrCall, data: D): R = visitElement(expression, data)

    override fun visitConstructorCall(expression: IrConstructorCall, data: D): R = visitElement(expression, data)

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: D): R = visitElement(expression, data)

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: D): R = visitElement(expression, data)

    override fun visitGetClass(expression: IrGetClass, data: D): R = visitElement(expression, data)

    override fun visitCallableReference(expression: IrCallableReference, data: D): R = visitElement(expression, data)

    override fun visitFunctionReference(expression: IrFunctionReference, data: D): R = visitElement(expression, data)

    override fun visitPropertyReference(expression: IrPropertyReference, data: D): R = visitElement(expression, data)

    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference, data: D): R =
        visitElement(expression, data)

    override fun visitFunctionExpression(expression: IrFunctionExpression, data: D): R = visitElement(expression, data)

    override fun visitClassReference(expression: IrClassReference, data: D): R = visitElement(expression, data)

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: D): R = visitElement(expression, data)

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: D): R = visitElement(expression, data)

    override fun visitWhen(expression: IrWhen, data: D): R = visitElement(expression, data)

    override fun visitBranch(branch: IrBranch, data: D): R = visitElement(branch, data)

    override fun visitElseBranch(branch: IrElseBranch, data: D): R = visitElement(branch, data)

    override fun visitLoop(loop: IrLoop, data: D): R = visitElement(loop, data)

    override fun visitWhileLoop(loop: IrWhileLoop, data: D): R = visitElement(loop, data)

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: D): R = visitElement(loop, data)

    override fun visitTry(aTry: IrTry, data: D): R = visitElement(aTry, data)

    override fun visitCatch(aCatch: IrCatch, data: D): R = visitElement(aCatch, data)

    override fun visitBreakContinue(jump: IrBreakContinue, data: D): R = visitElement(jump, data)

    override fun visitBreak(jump: IrBreak, data: D): R = visitElement(jump, data)

    override fun visitContinue(jump: IrContinue, data: D): R = visitElement(jump, data)

    override fun visitReturn(expression: IrReturn, data: D): R = visitElement(expression, data)

    override fun visitThrow(expression: IrThrow, data: D): R = visitElement(expression, data)

    override fun visitDynamicExpression(expression: IrDynamicExpression, data: D): R = visitElement(expression, data)

    override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression, data: D): R = visitElement(expression, data)

    override fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression, data: D): R = visitElement(expression, data)

    override fun visitErrorDeclaration(declaration: IrErrorDeclaration, data: D): R = visitElement(declaration, data)

    override fun visitErrorExpression(expression: IrErrorExpression, data: D): R = visitElement(expression, data)

    override fun visitErrorCallExpression(expression: IrErrorCallExpression, data: D): R = visitElement(expression, data)
}
