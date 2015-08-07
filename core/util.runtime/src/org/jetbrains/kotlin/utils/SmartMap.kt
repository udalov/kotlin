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

package org.jetbrains.kotlin.utils

import java.util.*

/**
 * A map which maintains the order in which the elements were added and is optimized for small sizes.
 * This map is not synchronized and it does not support [remove]. Its [entries] is a set
 * which is also not synchronized and does not support any modification operations.
 */
@Suppress("UNCHECKED_CAST")
// We don't inherit from AbstractMap because it has two additional fields (keySet and values)
class SmartMap<K, V> private constructor() : MutableMap<K, V> {
    companion object {
        private val ARRAY_THRESHOLD = 5
        private val EMPTY_ARRAY = arrayOf<Any?>()

        @JvmStatic
        fun <K, V> create(): SmartMap<K, V> = SmartMap()

        @JvmStatic
        fun <K, V> create(map: Map<K, V>): SmartMap<K, V> = SmartMap<K, V>().apply { this.putAll(map) }
    }

    // Array of alternating keys and values if size < threshold, hash map otherwise
    private var data: Any? = EMPTY_ARRAY

    override var size: Int = 0

    override fun put(key: K, value: V): V? {
        if (size >= ARRAY_THRESHOLD) {
            val map = data as MutableMap<K, V>
            return map.put(key, value).apply {
                size = map.size
            }
        }

        val arr = data as Array<Any?>
        for (i in 0..size - 1) {
            if (arr[2 * i] == key) {
                return (arr[2 * i + 1] as V).apply {
                    arr[2 * i + 1] = value
                }
            }
        }

        data = if (size == ARRAY_THRESHOLD - 1)
            hashMapOf(
                    *(arr.indices step 2).map { i -> arr[i] to arr[i + 1] }.toTypedArray(),
                    key to value
            )
        else Arrays.copyOf(arr, arr.size + 2).apply {
            set(size - 2, key)
            set(size - 1, value)
        }

        size++
        return null
    }

    override fun get(key: K): V? {
        if (size >= ARRAY_THRESHOLD) {
            val map = data as MutableMap<K, V>
            return map[key]
        }

        val arr = data as Array<Any?>
        for (i in 0..size - 1) {
            if (arr[2 * i] == key) {
                return arr[2 * i + 1] as V?
            }
        }

        return null
    }

    override fun remove(key: K): V? =
            throw UnsupportedOperationException("This map does not support 'remove'.")

    override fun putAll(from: Map<out K, V>) =
            from.entries.forEach { put(it.key, it.value) }

    override fun clear() {
        data = EMPTY_ARRAY
        size = 0
    }

    override fun isEmpty(): Boolean = size == 0

    override fun containsKey(key: K): Boolean = key in keys

    override fun containsValue(value: V): Boolean = value in values

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
            get() = if (size < ARRAY_THRESHOLD) ArrayEntrySet() else (data as MutableMap<K, V>).entries

    override val keys: MutableSet<K>
            get() = if (size < ARRAY_THRESHOLD) ArrayKeySet() else (data as MutableMap<K, V>).keys

    override val values: MutableCollection<V>
            get() = if (size < ARRAY_THRESHOLD) ArrayValues() else (data as MutableMap<K, V>).values

    override fun hashCode(): Int =
            entries.hashCode()

    override fun equals(other: Any?) =
            other is Map<*, *> && entries == other.entries

    private inner class ArrayEntrySet : AbstractSet<MutableMap.MutableEntry<K, V>>() {
        override val size: Int get() = this@SmartMap.size

        override fun iterator() = object : MutableIterator<MutableMap.MutableEntry<K, V>> {
            private var i = 0

            override fun next(): MutableMap.MutableEntry<K, V> {
                val arr = data as Array<Any?>
                val i = i.apply { i += 2 }
                if (i >= arr.size) throw NoSuchElementException()
                return AbstractMap.SimpleEntry(arr[i] as K, arr[i + 1] as V)
            }

            override fun hasNext(): Boolean =
                    i < (data as Array<Any?>).size

            override fun remove(): Unit =
                    throw UnsupportedOperationException("This iterator does not support 'remove'.")
        }
    }

    private inner class ArrayKeySet : AbstractSet<K>() {
        override val size: Int get() = this@SmartMap.size

        override fun iterator(): MutableIterator<K> = object : MutableIterator<K> {
            private var it = entries.iterator()

            override fun next() = it.next().key

            override fun hasNext() = it.hasNext()

            override fun remove() = it.remove()
        }
    }

    private inner class ArrayValues : AbstractCollection<V>() {
        override val size: Int get() = this@SmartMap.size

        override fun iterator(): MutableIterator<V> = object : MutableIterator<V> {
            private var it = entries.iterator()

            override fun next() = it.next().value

            override fun hasNext() = it.hasNext()

            override fun remove() = it.remove()
        }
    }
}
