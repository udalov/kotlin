// LANGUAGE: +JvmOptimizedNullCastMessages
// TARGET_BACKEND: JVM
// WITH_STDLIB
package test

import kotlin.test.assertEquals

private fun test(f: () -> Unit): String? =
    try {
        f()
        throw AssertionError("Fail: no NPE")
    } catch (e: NullPointerException) {
        val message = e.message!!
        val prefix = "null cannot be cast to non-null type "
        if (!message.startsWith(prefix)) {
            throw AssertionError("Fail: unexpected exception message: ${e.message}", e)
        }
        message.substringAfter(prefix)
    }

inline fun <reified T : Any> Any?.cast(): T = this as T

// fun <U : Any> Any?.castUnchecked(): U = this as U

class C {
    class Nested
}

fun box(): String {
    val nil: Any? = null

    assertEquals("test.C", test { nil as C })
    assertEquals("test.C.Nested", test { nil as C.Nested })
    assertEquals("kotlin.String", test { nil as String })
    assertEquals("kotlin.Int", test { nil as Int })
    assertEquals("kotlin.IntArray", test { nil as IntArray })
    assertEquals("kotlin.Array", test { nil as Array<Int> })
    assertEquals("kotlin.collections.List", test { nil as List<C> })
    assertEquals("kotlin.collections.List", test { nil as MutableList<C> })

    // TODO: instead of U::class, take representative upper bound or smth, and generate it instead, in TypeOperatorLowering.
    // assertEquals("U", test { nil.castUnchecked<Number>() })

    assertEquals("test.C", test { nil.cast<C>() })
    assertEquals("test.C.Nested", test { nil.cast<C.Nested>() })
    assertEquals("kotlin.String", test { nil.cast<String>() })
    assertEquals("kotlin.Int", test { nil.cast<Int>() })
    assertEquals("kotlin.IntArray", test { nil.cast<IntArray>() })
    assertEquals("kotlin.Array", test { nil.cast<Array<Int>>() })
    assertEquals("kotlin.collections.List", test { nil.cast<List<C>>() })
    assertEquals("kotlin.collections.List", test { nil.cast<MutableList<C>>() })

    return "OK"
}
