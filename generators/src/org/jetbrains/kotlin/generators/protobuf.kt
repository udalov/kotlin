/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

@file:Suppress("UNCHECKED_CAST")
package org.jetbrains.kotlin.generators

import com.google.protobuf.Descriptors
import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.Message
import org.jetbrains.kotlin.preloading.ClassPreloadingUtils
import org.jetbrains.kotlin.serialization.DebugProtoBuf
import org.jetbrains.kotlin.serialization.builtins.DebugBuiltInsProtoBuf
import org.jetbrains.kotlin.serialization.jvm.BitEncoding
import org.jetbrains.kotlin.serialization.jvm.DebugJvmProtoBuf
import java.io.File
import java.util.*
import java.util.zip.ZipFile

val JVM_EXTENSIONS: ExtensionRegistry =
        ExtensionRegistry.newInstance().apply {
            DebugJvmProtoBuf.registerAllExtensions(this)
        }

val BUILTINS_EXTENSIONS: ExtensionRegistry =
        ExtensionRegistry.newInstance().apply {
            DebugBuiltInsProtoBuf.registerAllExtensions(this)
        }

fun main(args: Array<String>) {
/*
    val base = File("dist/builtins2")
    for (builtin in File(base, "kotlin").listFiles { it.name.endsWith(".kotlin_class") } ?: arrayOf()) {
        println(builtin.relativeTo(base))
        val message = DebugProtoBuf.Class.parseFrom(builtin.readBytes(), BUILTINS_EXTENSIONS)
        go(Descriptors.FieldDescriptor.Type.MESSAGE, message.descriptorForType.name, message, 2)
    }
    return
*/

    val file = File("dist/kotlinc/lib/kotlin-runtime.jar")
    val classLoader = ClassPreloadingUtils.preloadClasses(listOf(file), 4096, null, null)

    /*
    File("dist/kotlinc/lib/kotlin-runtime.jar").copyTo(File("dist/kotlin-runtime.jar"), overwrite = true)
    val file = File("dist/kotlin-reflect-before-jarjar.jar")
    val classLoader = ClassPreloadingUtils.preloadClasses(listOf(file), 4096, null, null)
    */

    val entries = ZipFile(file).entries().toList().map { it.name }

    var totalDataSize = 0
    var totalStringsSize = 0
    var totalSize = 0

    val byPercentage = TreeMap<Double, MutableList<Class<*>>>()

    val classNames = entries.filter { it.endsWith(".class") }.map { it.removeSuffix(".class").replace('/', '.') }
    loop@for (className in classNames) {
        val klass = try { classLoader.loadClass(className) } catch (e: NullPointerException) { continue }
        val anno = klass.annotations.firstOrNull { it.annotationType().canonicalName.startsWith("kotlin.jvm.internal.Kotlin") } ?: continue
        val dataMethod = try { anno.annotationType().getMethod("data") } catch (e: NoSuchMethodException) { continue }
        val data = dataMethod.invoke(anno) as Array<String>
        val strings = anno.annotationType().getMethod("strings").invoke(anno) as Array<String>

        println("class $className")
        val bytes = BitEncoding.decodeBytes(data)
        val dataSize = bytes.size()
        val fileSize = classLoader.getResource(className.replace('.', '/') + ".class").openStream().readBytes().size()
        val percent = dataSize * 100.0 / fileSize

        totalDataSize += data.sumBy { it.toByteArray().size() }
        totalStringsSize += strings.sumBy { it.toByteArray().size() }
        totalSize += fileSize

        val input = bytes.inputStream()
        val stringTableTypes = DebugJvmProtoBuf.StringTableTypes.parseDelimitedFrom(input, JVM_EXTENSIONS)

        println("  data $dataSize / $fileSize bytes (${percent.str()}), ${strings.size()} strings, ${stringTableTypes.serializedSize} bytes for types")

        byPercentage.getOrPut(percent, { ArrayList() }).add(klass)

        val parser = when (anno.annotationType().simpleName) {
            // "KotlinClass" -> DebugProtoBuf.Class.PARSER
            "KotlinMultifileClassPart" -> DebugProtoBuf.Package.PARSER
            else -> continue@loop
        }
        val message = parser.parseFrom(input, JVM_EXTENSIONS)

        sizeByName.clear()
        go(Descriptors.FieldDescriptor.Type.MESSAGE, message.descriptorForType.name, message, 2)

        println("----------")
        go(Descriptors.FieldDescriptor.Type.MESSAGE, stringTableTypes.descriptorForType.name, stringTableTypes, 2)

        println("##########")
        for ((count, fieldName) in sizeByName.entrySet().map { it.value to it.key }.sortedByDescending { it.first }) {
            println("            $count $fieldName")
        }
        println("##########")
    }

    repeat(5) { println() }

    for ((percent, klass) in byPercentage.descendingMap().entrySet().flatMap { entry ->
        val (percent, classes) = entry
        classes.map { percent to it }
    }) {
        println("${percent.str()} ${klass.name}")
    }

    println()
    println("total file size: $totalSize")
    println("total 'data' size: $totalDataSize (${(totalDataSize * 100.0 / totalSize).str()})")
    println("total 'strings' size: $totalStringsSize (${(totalStringsSize * 100.0 / totalSize).str()})")
    println("total 'data' + 'strings' size: ${totalDataSize + totalStringsSize} (${((totalDataSize + totalStringsSize) * 100.0 / totalSize).str()})")
}

fun Double.str() = "%02.03f".format(this) + "%"

val sizeByName = hashMapOf<String, Int>()

fun go(type: Descriptors.FieldDescriptor.Type, name: String, value: Any, indent: Int) {
    fun printFields(message: Message, indent: Int) {
        for ((subKey, subValue) in message.allFields) {
            go(subKey.type, subKey.name, subValue, indent)
        }
        for ((unknownTag, unknownField) in message.unknownFields.asMap()) {
            println(" ".repeat(indent) + "UNKNOWN " + unknownField + " (tag " + unknownTag + ")")
        }
    }

    print(" ".repeat(indent) + type + " " + name)

    if (value is Message) {
        print(" " + value.serializedSize)
        println()
        printFields(value, indent + 2)
        sizeByName[name] = (sizeByName[name] ?: 0) + value.serializedSize
    }
    else if (value is List<*> && value.first() is Message) {
        val messages = value as List<Message>
        print(" " + messages.sumBy { it.serializedSize } + " (${messages.size()} messages)")
        println()
        for ((index, message) in messages.withIndex()) {
            println(" ".repeat(indent + 2) + "#$index ${message.serializedSize}")
            printFields(message, indent + 4)
        }
        sizeByName[name] = (sizeByName[name] ?: 0) + messages.sumBy { it.serializedSize }
    }
    else if (value is Int || value is Long || value is String) {
        println(" (value = $value)")
    }
    else if (value is Descriptors.EnumValueDescriptor) {
        println(" (value = ${value.name})")
    }
    else {
        println()
    }
}
