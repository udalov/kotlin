/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.reflect

import junit.framework.TestCase
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.runtime.components.ReflectKotlinClassFinder
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInsPackageFragmentImpl
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import java.net.URLClassLoader
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.test.assertIs

class BuiltInsResourceLoaderTest : TestCase() {
    fun testParallelAccess() {
        assertEquals("1.8", System.getProperty("java.specification.version"))

        val classLoader = ForTestCompileRuntime.runtimeAndReflectJarClassLoader()
        assertIs<URLClassLoader>(classLoader)

        val latch = CountDownLatch(1)
        val threads = (0 until 1000).map {
            // Construct a bunch of kotlin-reflect internals.
            val packageFqName = FqName("kotlin")
            val finder = ReflectKotlinClassFinder(classLoader)
            val storageManager = LockBasedStorageManager("BuiltInsResourceLoaderTest")
            val builtIns = JvmBuiltIns(storageManager, JvmBuiltIns.Kind.FROM_DEPENDENCIES)
            val module = ModuleDescriptorImpl(Name.special("<test>"), storageManager, builtIns)

            // This line opens an input stream via ClassLoader.getResourceAsStream or ClassLoader.getSystemResourceAsStream.
            val stream = finder.findBuiltInsData(packageFqName)!!
            thread(start = false) {
                latch.await()
                // This line parses protobuf from the input stream and closes the input stream.
                BuiltInsPackageFragmentImpl.create(packageFqName, storageManager, module, stream, isFallback = false)
            }
        }

        threads.forEach(Thread::start)
        latch.countDown()
        threads.forEach(Thread::join)
    }
}
