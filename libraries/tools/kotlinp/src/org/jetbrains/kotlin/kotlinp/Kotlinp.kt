/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp

import kotlinx.metadata.InconsistentKotlinMetadataException
import kotlinx.metadata.impl.accept
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.KotlinModuleMetadata
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.metadata.builtins.BuiltInsProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import java.io.File

class Kotlinp(val settings: KotlinpSettings) {
    internal fun renderKotlinMetadataClassFile(file: File): String {
        val stream = file.inputStream()
        BuiltInsBinaryVersion.readFrom(stream)
        val message = ProtoBuf.PackageFragment.parseFrom(
            stream,
            ExtensionRegistryLite.newInstance().apply { BuiltInsProtoBuf.registerAllExtensions(this) }
        )
        val nameResolver = NameResolverImpl(message.strings, message.qualifiedNames)
        return buildString {
            for (klass in message.class_List) {
                append(ClassPrinter(settings).print { v ->
                    klass.accept(v, nameResolver)
                })
                appendln()
            }
        }
    }

    internal fun renderClassFile(classFile: KotlinClassMetadata?): String =
        when (classFile) {
            is KotlinClassMetadata.Class -> ClassPrinter(settings).print(classFile)
            is KotlinClassMetadata.FileFacade -> FileFacadePrinter(settings).print(classFile)
            is KotlinClassMetadata.SyntheticClass -> {
                if (classFile.isLambda) LambdaPrinter(settings).print(classFile)
                else buildString { appendln("synthetic class") }
            }
            is KotlinClassMetadata.MultiFileClassFacade -> MultiFileClassFacadePrinter().print(classFile)
            is KotlinClassMetadata.MultiFileClassPart -> MultiFileClassPartPrinter(settings).print(classFile)
            is KotlinClassMetadata.Unknown -> buildString { appendln("unknown file (k=${classFile.header.kind})") }
            null -> buildString { appendln("unsupported file") }
        }

    internal fun readClassFile(file: File): KotlinClassMetadata? {
        val header = file.readKotlinClassHeader() ?: throw KotlinpException("file is not a Kotlin class file: $file")
        return try {
            KotlinClassMetadata.read(header)
        } catch (e: InconsistentKotlinMetadataException) {
            throw KotlinpException("inconsistent Kotlin metadata: ${e.message}")
        }
    }

    internal fun renderModuleFile(metadata: KotlinModuleMetadata?): String =
        if (metadata != null) ModuleFilePrinter().print(metadata)
        else buildString { appendln("unsupported file") }

    internal fun readModuleFile(file: File): KotlinModuleMetadata? =
        KotlinModuleMetadata.read(file.readBytes())
}

data class KotlinpSettings(
    val isVerbose: Boolean
)
