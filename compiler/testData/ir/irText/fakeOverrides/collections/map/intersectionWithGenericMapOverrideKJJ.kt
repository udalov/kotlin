// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB

// K1-based kotlin-reflect does not render definitely non-nullable types of parameters of `MutableMap.merge` and
// `MutableMap.computeIfPresent`, which are substituted with a type parameter of the subclass: `T` instead of `T & Any`.
// KOTLIN_REFLECT_DUMP_MISMATCH

// FILE: 1.kt
import java.util.HashMap
import java.util.SortedMap

abstract class A<T> : SortedMap<T, T>, HashMap<T, T>()

abstract class B<T> : SortedMap<T, T>, HashMap<T, T>() {
    override fun put(key: T, value: T): T? {
        return null!!
    }

    override fun remove(key: T): T {
        return null!!
    }
}

fun test(a: A<Boolean>, b: B<Boolean?>) {
    a.size
    a[true] = true
    a.put(null, null)
    a.get(true)
    a.get(null)
    a.remove(null)
    a.remove(true)

    b.put(false, false)
    b[true] = true
    b.remove(null)
}