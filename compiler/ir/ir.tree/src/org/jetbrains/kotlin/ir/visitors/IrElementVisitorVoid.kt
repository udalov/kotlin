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

package org.jetbrains.kotlin.ir.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*

abstract class IrElementVisitorVoid {
    abstract fun visitElement(element: IrElement)

    open fun visitModuleFragment(declaration: IrModuleFragment) = visitElement(declaration)

    open fun visitPackageFragment(declaration: IrPackageFragment) = visitElement(declaration)

    open fun visitExternalPackageFragment(declaration: IrExternalPackageFragment) = visitPackageFragment(declaration)

    open fun visitFile(declaration: IrFile) = visitPackageFragment(declaration)

    open fun visitDeclaration(declaration: IrDeclaration) = visitElement(declaration)

    open fun visitClass(declaration: IrClass) = visitDeclaration(declaration)

    open fun visitScript(declaration: IrScript) = visitDeclaration(declaration)

    open fun visitFunction(declaration: IrFunction) = visitDeclaration(declaration)

    open fun visitSimpleFunction(declaration: IrSimpleFunction) = visitFunction(declaration)

    open fun visitConstructor(declaration: IrConstructor) = visitFunction(declaration)

    open fun visitProperty(declaration: IrProperty) = visitDeclaration(declaration)

    open fun visitField(declaration: IrField) = visitDeclaration(declaration)

    open fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty) = visitDeclaration(declaration)

    open fun visitVariable(declaration: IrVariable) = visitDeclaration(declaration)

    open fun visitEnumEntry(declaration: IrEnumEntry) = visitDeclaration(declaration)

    open fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) = visitDeclaration(declaration)

    open fun visitTypeParameter(declaration: IrTypeParameter) = visitDeclaration(declaration)

    open fun visitValueParameter(declaration: IrValueParameter) = visitDeclaration(declaration)

    open fun visitTypeAlias(declaration: IrTypeAlias) = visitDeclaration(declaration)

    open fun visitBody(body: IrBody) = visitElement(body)

    open fun visitExpressionBody(body: IrExpressionBody) = visitBody(body)

    open fun visitBlockBody(body: IrBlockBody) = visitBody(body)

    open fun visitSyntheticBody(body: IrSyntheticBody) = visitBody(body)

    open fun visitSuspendableExpression(expression: IrSuspendableExpression) = visitExpression(expression)

    open fun visitSuspensionPoint(expression: IrSuspensionPoint) = visitExpression(expression)

    open fun visitExpression(expression: IrExpression) = visitElement(expression)

    open fun <T> visitConst(expression: IrConst<T>) = visitExpression(expression)

    open fun visitVararg(expression: IrVararg) = visitExpression(expression)

    open fun visitSpreadElement(spread: IrSpreadElement) = visitElement(spread)

    open fun visitContainerExpression(expression: IrContainerExpression) = visitExpression(expression)

    open fun visitComposite(expression: IrComposite) = visitContainerExpression(expression)

    open fun visitBlock(expression: IrBlock) = visitContainerExpression(expression)

    open fun visitStringConcatenation(expression: IrStringConcatenation) = visitExpression(expression)

    open fun visitDeclarationReference(expression: IrDeclarationReference) = visitExpression(expression)

    open fun visitSingletonReference(expression: IrGetSingletonValue) = visitDeclarationReference(expression)

    open fun visitGetObjectValue(expression: IrGetObjectValue) = visitSingletonReference(expression)

    open fun visitGetEnumValue(expression: IrGetEnumValue) = visitSingletonReference(expression)

    open fun visitVariableAccess(expression: IrValueAccessExpression) = visitDeclarationReference(expression)

    open fun visitGetValue(expression: IrGetValue) = visitVariableAccess(expression)

    open fun visitSetVariable(expression: IrSetVariable) = visitVariableAccess(expression)

    open fun visitFieldAccess(expression: IrFieldAccessExpression) = visitDeclarationReference(expression)

    open fun visitGetField(expression: IrGetField) = visitFieldAccess(expression)

    open fun visitSetField(expression: IrSetField) = visitFieldAccess(expression)

    open fun visitMemberAccess(expression: IrMemberAccessExpression) = visitExpression(expression)

    open fun visitFunctionAccess(expression: IrFunctionAccessExpression) = visitMemberAccess(expression)

    open fun visitCall(expression: IrCall) = visitFunctionAccess(expression)

    open fun visitConstructorCall(expression: IrConstructorCall) = visitFunctionAccess(expression)

    open fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) = visitFunctionAccess(expression)

    open fun visitEnumConstructorCall(expression: IrEnumConstructorCall) = visitFunctionAccess(expression)

    open fun visitGetClass(expression: IrGetClass) = visitExpression(expression)

    open fun visitCallableReference(expression: IrCallableReference) = visitMemberAccess(expression)

    open fun visitFunctionReference(expression: IrFunctionReference) = visitCallableReference(expression)

    open fun visitPropertyReference(expression: IrPropertyReference) = visitCallableReference(expression)

    open fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference) = visitCallableReference(expression)

    open fun visitFunctionExpression(expression: IrFunctionExpression) = visitExpression(expression)

    open fun visitClassReference(expression: IrClassReference) = visitDeclarationReference(expression)

    open fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall) = visitExpression(expression)

    open fun visitTypeOperator(expression: IrTypeOperatorCall) = visitExpression(expression)

    open fun visitWhen(expression: IrWhen) = visitExpression(expression)

    open fun visitBranch(branch: IrBranch) = visitElement(branch)

    open fun visitElseBranch(branch: IrElseBranch) = visitBranch(branch)

    open fun visitLoop(loop: IrLoop) = visitExpression(loop)

    open fun visitWhileLoop(loop: IrWhileLoop) = visitLoop(loop)

    open fun visitDoWhileLoop(loop: IrDoWhileLoop) = visitLoop(loop)

    open fun visitTry(aTry: IrTry) = visitExpression(aTry)

    open fun visitCatch(aCatch: IrCatch) = visitElement(aCatch)

    open fun visitBreakContinue(jump: IrBreakContinue) = visitExpression(jump)

    open fun visitBreak(jump: IrBreak) = visitBreakContinue(jump)

    open fun visitContinue(jump: IrContinue) = visitBreakContinue(jump)

    open fun visitReturn(expression: IrReturn) = visitExpression(expression)

    open fun visitThrow(expression: IrThrow) = visitExpression(expression)

    open fun visitDynamicExpression(expression: IrDynamicExpression) = visitExpression(expression)

    open fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression) = visitDynamicExpression(expression)

    open fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression) = visitDynamicExpression(expression)

    open fun visitErrorDeclaration(declaration: IrErrorDeclaration) = visitDeclaration(declaration)

    open fun visitErrorExpression(expression: IrErrorExpression) = visitExpression(expression)

    open fun visitErrorCallExpression(expression: IrErrorCallExpression) = visitErrorExpression(expression)
}

// This is temporary just to avoid unresolved references in imports
fun acceptVoid() {}
fun acceptChildrenVoid() {}
