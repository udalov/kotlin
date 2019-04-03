// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// WITH_REFLECT
// MODULE: m1
// FILE: A.kt

package lib

@JvmName("bar")
fun foo() = "foo"

var v: Int = 1
    @JvmName("vget")
    get
    @JvmName("vset")
    set

fun consumeInt(x: Int) {}

class A {
    val OK: String = "OK"
        @JvmName("OK") get
}

annotation class Anno(@get:JvmName("uglyJvmName") val value: String)

// MODULE: m2(m1)
// FILE: B.kt

import lib.*

@Anno("OK")
fun annotated() {}

fun box(): String {
    foo()
    v = 1
    consumeInt(v)

    val annoValue = (::annotated.annotations.single() as Anno).value
    if (annoValue != "OK") return "Fail annotation value: $annoValue"

    return A().OK
}
