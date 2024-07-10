/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.loops.ForLoopsLowering
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.jvm.lower.*
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import java.lang.reflect.ParameterizedType

fun main() {
    var total = 0
    var common = 0
    for (phase in jvmFilePhases0 + jvmModulePhases) {
        val kFunctionType = phase.javaClass.genericInterfaces[0] as ParameterizedType
        val lowering = kFunctionType.actualTypeArguments[1] as Class<*>
        val supers = generateSequence(lowering) { klass ->
            klass.superclass.takeUnless {
                it == Any::class.java ||
                        it.simpleName.contains("IrElementTransformerVoid") ||
                        it.simpleName.contains("IrBuildingTransformer")
            }
        }.toList()

        if (supers.any {
                it.name.startsWith("org.jetbrains.kotlin.backend.common.lower") ||
                        it.name.startsWith("org.jetbrains.kotlin.ir.inline")
            }) {
            common++
            print("  ")
        }
        total++

        println(supers)
    }

    println()
    println("total $total common $common")
}

private val jvmFilePhases0 = listOf<(JvmBackendContext) -> FileLoweringPass>(
    ::TypeAliasAnnotationMethodsLowering,
    ::ProvisionalFunctionExpressionLowering,

    ::JvmOverloadsAnnotationLowering,
    ::MainMethodGenerationLowering,

    ::AnnotationLowering,
    ::JvmAnnotationImplementationLowering,
    ::PolymorphicSignatureLowering,
    ::VarargLowering,

    ::JvmLateinitLowering,
    ::JvmInventNamesForLocalClasses,

    ::JvmInlineCallableReferenceToLambdaPhase,
    ::DirectInvokeLowering,
    ::FunctionReferenceLowering,

    ::SuspendLambdaLowering,
    ::PropertyReferenceDelegationLowering,
    ::SingletonOrConstantDelegationLowering,
    ::PropertyReferenceLowering,
    ::ArrayConstructorLowering,

    // TODO: merge the next three phases together, as visitors behave incorrectly between them
    //  (backing fields moved out of companion objects are reachable by two paths):
    ::MoveOrCopyCompanionObjectFieldsLowering,
    ::JvmPropertiesLowering,
    ::RemapObjectFieldAccesses,

    ::AnonymousObjectSuperConstructorLowering,
    ::JvmBuiltInsLowering,

    ::RangeContainsLowering,
    ::ForLoopsLowering,
    ::CollectionStubMethodLowering,
    ::JvmSingleAbstractMethodLowering,
    ::JvmMultiFieldValueClassLowering,
    ::JvmInlineClassLowering,
    ::JvmTailrecLowering,

    ::MappedEnumWhenLowering,

    ::AssertionLowering,
    ::JvmReturnableBlockLowering,
    ::SingletonReferencesLowering,
    ::SharedVariablesLowering,
    ::JvmLocalDeclarationsLowering,

    ::RemoveDuplicatedInlinedLocalClassesLowering,
    ::JvmInventNamesForInlinedAnonymousObjects,

    ::JvmLocalClassPopupLowering,
    ::StaticCallableReferenceLowering,

    ::JvmDefaultConstructorLowering,

    ::FlattenStringConcatenationLowering,
    ::JvmStringConcatenationLowering,

    ::JvmDefaultArgumentStubGenerator,
    ::JvmDefaultParameterInjector,
    ::JvmDefaultParameterCleaner,

    ::FragmentLocalFunctionPatchLowering,

    ::InterfaceLowering,
    ::InheritedDefaultMethodsOnClassesLowering,
    ::ReplaceDefaultImplsOverriddenSymbols,
    ::InterfaceSuperCallsLowering,
    ::InterfaceDefaultCallsLowering,
    ::InterfaceObjectCallsLowering,

    ::TailCallOptimizationLowering,
    ::AddContinuationLowering,

    ::JvmInnerClassesLowering,
    ::JvmInnerClassesMemberBodyLowering,
    ::JvmInnerClassConstructorCallsLowering,

    ::EnumClassLowering,
    ::EnumExternalEntriesLowering,
    ::ObjectClassLowering,
    ::StaticInitializersLowering,
    ::UniqueLoopLabelsLowering,
    ::JvmInitializersLowering,
    ::JvmInitializersCleanupLowering,
    ::FunctionNVarargBridgeLowering,
    ::JvmStaticInCompanionLowering,
    ::StaticDefaultFunctionLowering,
    ::BridgeLowering,
    ::SyntheticAccessorLowering,

    ::JvmArgumentNullabilityAssertionsLowering,
    ::ToArrayLowering,
    ::JvmSafeCallChainFoldingLowering,
    ::JvmOptimizationLowering,
    ::AdditionalClassAnnotationLowering,
    ::RecordEnclosingMethodsLowering,
    ::TypeOperatorLowering,
    ::ReplaceKFunctionInvokeWithFunctionInvoke,
    ::JvmKotlinNothingValueExceptionLowering,
    ::MakePropertyDelegateMethodsStaticLowering,
    ::ReplaceNumberToCharCallSitesLowering,

    ::RenameFieldsLowering,
    ::FakeLocalVariablesForBytecodeInlinerLowering,
    ::FakeLocalVariablesForIrInlinerLowering,

    ::ReflectiveAccessLowering,
)
private val jvmFilePhases = createFilePhases<JvmBackendContext>(*jvmFilePhases0.toTypedArray())

private val jvmModulePhases = listOf<(JvmBackendContext) -> ModuleLoweringPass>(
    ::ExternalPackageParentPatcherLowering,
    ::FragmentSharedVariablesLowering,
    ::JvmIrValidationBeforeLoweringPhase,
    ::ProcessOptionalAnnotations,
    ::JvmExpectDeclarationRemover,
    ::ConstEvaluationLowering,
    ::SerializeIrPhase,
    ::ScriptsToClassesLowering,
    ::FileClassLowering,
    ::JvmStaticInObjectLowering,
    ::RepeatedAnnotationLowering,
    ::JvmInlineCallableReferenceToLambdaWithDefaultsPhase,

    ::JvmIrInliner,
    ::ApiVersionIsAtLeastEvaluationLowering,
    ::CreateSeparateCallForInlinedLambdasLowering,
    ::MarkNecessaryInlinedClassesAsRegeneratedLowering,
    ::InlinedClassReferencesBoxingLowering,
    ::RestoreInlineLambda,
)

val jvmLoweringPhases = SameTypeNamedCompilerPhase(
    name = "IrLowering",
    description = "IR lowering",
    nlevels = 1,
    actions = DEFAULT_IR_ACTIONS,
    lower = buildModuleLoweringsPhase(
        *jvmModulePhases.toTypedArray()
    ).then(
        performByIrFile("PerformByIrFile", lower = jvmFilePhases)
    ) then buildModuleLoweringsPhase(
        ::GenerateMultifileFacades,
        ::ResolveInlineCalls,
        ::JvmIrValidationAfterLoweringPhase
    )
)

private typealias JvmPhase = CompilerPhase<JvmBackendContext, IrModuleFragment, IrModuleFragment>

private fun buildModuleLoweringsPhase(vararg phases: (JvmBackendContext) -> ModuleLoweringPass): JvmPhase =
    createModulePhases(*phases).reduce(JvmPhase::then)
