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

package jet.objc

public abstract class ObjCObject protected(private val pointer: Long) {
    class object {
        public val NIL: Nil = Nil.INSTANCE
    }

    override fun toString(): String = "[ObjCObject %s %016x]".format(javaClass.getName(), pointer)
    override fun hashCode(): Int = (pointer xor (pointer ushr 32)).toInt()
    override fun equals(other: Any?): Boolean = other is ObjCObject && other.pointer == pointer
}
