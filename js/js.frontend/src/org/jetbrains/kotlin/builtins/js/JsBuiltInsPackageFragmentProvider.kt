/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins.js

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.descriptors.deserialization.AdditionalClassPartsProvider
import org.jetbrains.kotlin.descriptors.deserialization.PlatformDependentDeclarationFilter
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.serialization.deserialization.KotlinMetadataFinder
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInsPackageFragmentProvider
import org.jetbrains.kotlin.storage.StorageManager

class JsBuiltInsPackageFragmentProvider(
    storageManager: StorageManager,
    finder: KotlinMetadataFinder,
    moduleDescriptor: ModuleDescriptor,
    notFoundClasses: NotFoundClasses,
    deserializationConfiguration: DeserializationConfiguration
) : BuiltInsPackageFragmentProvider(
    storageManager, finder, moduleDescriptor, notFoundClasses, AdditionalClassPartsProvider.None, PlatformDependentDeclarationFilter.All,
    deserializationConfiguration
)
