/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins.jvm

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.descriptors.deserialization.AdditionalClassPartsProvider
import org.jetbrains.kotlin.descriptors.deserialization.ClassDescriptorFactory
import org.jetbrains.kotlin.descriptors.deserialization.PlatformDependentDeclarationFilter
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInsPackageFragmentProvider
import org.jetbrains.kotlin.storage.StorageManager

class JvmBuiltInsPackageFragmentProvider(
    storageManager: StorageManager,
    finder: KotlinClassFinder,
    moduleDescriptor: ModuleDescriptor,
    notFoundClasses: NotFoundClasses,
    additionalClassPartsProvider: AdditionalClassPartsProvider,
    platformDependentDeclarationFilter: PlatformDependentDeclarationFilter,
    deserializationConfiguration: DeserializationConfiguration
) : BuiltInsPackageFragmentProvider(
    storageManager, finder, moduleDescriptor, notFoundClasses, additionalClassPartsProvider, platformDependentDeclarationFilter,
    deserializationConfiguration
) {
    override fun getAdditionalClassDescriptorFactories(
        storageManager: StorageManager,
        moduleDescriptor: ModuleDescriptor
    ): List<ClassDescriptorFactory> {
        return listOf(JvmBuiltInClassDescriptorFactory(storageManager, moduleDescriptor))
    }
}
