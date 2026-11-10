// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB

// K1-based kotlin-reflect does not render definitely non-nullable types of parameters of `MutableMap.merge` and
// `MutableMap.computeIfPresent`, which are substituted with a type parameter of the subclass: `T` instead of `T & Any`.
// KOTLIN_REFLECT_DUMP_MISMATCH

// FILE: 1.kt
import java.util.SortedMap

abstract class C<T> : SortedMap<T, T>

abstract class D<T>(
    override val size: Int
) : SortedMap<T, T> {
    override fun remove(key: T): T? {
        return null
    }
}

fun test(c: C<Boolean>, d: D<Boolean>){
    c.size
    c[false] = false
    c[null] = null
    c.remove(null)
    c.remove(true)
    c.get(null)
    c.get(true)

    d.size
    d[false] = false
    d[null] = null
    d.remove(null)
    d.remove(true)
    d.get(null)
    d.get(true)
}