/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package kotlin.jvm.objc

public open class Pointer<T> internal(open val peer: Long) {
    class object {
        public val NULL: Pointer<Any> = Pointer(0)
        public val CHAR_SIZE: Int = 1

        public fun allocateChar(): Pointer<Char> = allocateChars(1)
        public fun allocateChars(size: Long): Pointer<Char> = Pointer<Char>(Native.malloc(size * CHAR_SIZE))
        public fun pointerToChar(char: Char): Pointer<Char> {
            val pointer = allocateChar()
            pointer.setChar(char)
            return pointer
        }

        // TODO: support different encodings and stuff
        public fun pointerToString(string: String): Pointer<Char> {
            // TODO: not very optimal, use a native function instead
            val n = string.length
            val pointer = allocateChars(n + 1L)
            for (i in n.indices) {
                pointer.setChar(i.toLong(), string[i])
            }
            pointer.setChar(n.toLong(), 0.toChar())
            return pointer
        }
    }

    public fun getChar(): Char = getChar(0)
    public fun getChar(offset: Long): Char = (Native.getWord(peer + offset) and 0xff).toChar()
    public fun setChar(char: Char): Unit = setChar(0, char)
    public fun setChar(offset: Long, char: Char): Unit = Native.setWord(peer + offset, char.toByte().toLong())

    public fun getString(): String {
        val sb = StringBuilder()
        var offset = 0L
        while (true) {
            val c = getChar(offset)
            if (c == 0.toChar()) break
            sb.append(c)
            offset++
        }
        return sb.toString()
    }

    public fun release(): Unit = Native.free(peer)

    override fun toString(): String = "[Pointer %016x]".format(peer)
    override fun hashCode(): Int = (peer xor (peer ushr 32)).toInt()
    override fun equals(other: Any?): Boolean = other is Pointer<*> && other.peer == peer
}
