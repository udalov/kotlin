// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB

// K1-based kotlin-reflect does not render definitely non-nullable types of parameters of `MutableMap.merge` and
// `MutableMap.computeIfPresent`, which are substituted with a type parameter of the subclass: `T` instead of `T & Any`.
// KOTLIN_REFLECT_DUMP_MISMATCH

// FILE: Java1.java
abstract public class Java1<T> extends KotlinClass<T> {}

// FILE: 1.kt
import java.util.SortedMap

abstract class A<T> : Java1<T>()

abstract class B<T> : Java1<T>() {
    override val size: Int
        get() = 5

    override fun get(key: T?): T? {
        return null!!
    }

    override fun isEmpty(): Boolean {
        return null!!
    }
}

abstract class KotlinClass<T> : SortedMap<T, T>

fun test(a: A<Boolean>, b: B<Boolean?>) {
    a.size
    a[true] = true
    a.put(null, null)
    a.get(true)
    a.get(null)
    a.remove(null)
    a.remove(true)
    a.isNotEmpty()

    b.size
    b.put(false, false)
    b.put(null, null)
    b[null] = null
    b[true] = true
    b.get(null)
    b.get(true)
    b.remove(null)
    b.remove(true)
    b.isEmpty()
}