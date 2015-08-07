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

package org.jetbrains.kotlin.util

import junit.framework.TestCase
import org.jetbrains.kotlin.utils.SmartMap
import java.util.*

public class SmartMapTest : TestCase() {
    fun testSimple() {
        val map = SmartMap.create<String, Int>()
        assertTrue(map.isEmpty())
        assertEquals(0, map.size)

        map["test"] = 42
        assertFalse(map.isEmpty())
        assertEquals(1, map.size)

        map["another"] = 0
        assertEquals(2, map.size)
    }

    fun testSimpleEntrySet() {
        val map = SmartMap.create<String, Int>()
        assertEquals(0, map.entries.size)
        assertTrue(map.entries.isEmpty())

        map["test"] = 42

        val entries = map.entries
        assertEquals(1, entries.size)
        assertEquals("test", entries.single().key)
        assertEquals(42, entries.single().value)
    }

    fun testEntrySetIsAView() {
        val map = SmartMap.create<String, Int>()
        val entries = map.entries
        map["test"] = 42
        assertEquals(1, entries.size)
        assertEquals(AbstractMap.SimpleImmutableEntry("test", 42), entries.single())
    }

    fun testClear() {
        val map = SmartMap.create<String, Int>()
        map["test"] = 42
        map["another"] = 0
        map.clear()
        assertEquals(0, map.size)
        assertTrue(map.isEmpty())
        map["after"] = 239
        assertEquals(239, map["after"])
    }

    fun testSubsequentAdds() {
        val map = SmartMap.create<String, Int>()
        for (i in 'a'..'z') {
            map[i.toString()] = i.toInt()
        }
        assertEquals(26, map.size)
        for (i in 'a'..'z') {
            assertEquals(i.toInt(), map[i.toString()])
        }
        for (c in 0..65535) {
            assertEquals(c.toChar() in 'a'..'z', map.containsKey(c.toChar().toString()))
            assertEquals(c.toChar() in 'a'..'z', map.containsValue(c.toInt()))
        }
    }

    fun testRandomAddsEqualsHashCode() {
        val hash = LinkedHashMap<Int, Int>()
        val smart = SmartMap.create<Int, Int>()
        val random = Random(42)
        for (i in 1..1000) {
            val key = random.nextInt(10)
            val value = random.nextInt(10)
            hash[key] = value
            smart[key] = value
        }
        assertEquals(hash, smart)
        assertEquals(hash.hashCode(), smart.hashCode())
        assertEquals(smart, hash)
        assertEquals(smart.hashCode(), hash.hashCode())
    }

    fun testKeysValues() {
        val map = SmartMap.create<Int, Int>()
        for (i in 1..1000) map[i] = i*i
        assertEquals(1000, map.keys.size)
        assertFalse(map.keys.isEmpty())
        assertEquals((1..1000).toList(), map.keys.toList())
        assertEquals(1000, map.values.size)
        assertFalse(map.values.isEmpty())
        assertEquals((1..1000).map { it * it }, map.values.toList())
    }

    fun testContainsKey() {
        val map = SmartMap.create<String, Int>()
        assertFalse(map.containsKey("a"))
        map["b"] = 2
        assertFalse(map.containsKey("a"))
        map["a"] = 1
        assertTrue(map.containsKey("a"))
        map.clear()
        assertFalse(map.containsKey("a"))
        for (c in 'a'..'z') map[c.toString()] = c.toInt()
        assertTrue(map.containsKey("a"))
    }

    fun testContainsValue() {
        val map = SmartMap.create<String, Int>()
        assertFalse(map.containsValue(1))
        map["b"] = 2
        assertFalse(map.containsValue(1))
        map["a"] = 1
        assertTrue(map.containsValue(1))
        map.clear()
        assertFalse(map.containsValue(1))
        for (c in 'a'..'z') map[c.toString()] = c.toInt()
        assertTrue(map.containsValue('a'.toInt()))
    }
}
