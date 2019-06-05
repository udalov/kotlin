// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: A.kt

import java.io.IOException

interface A {
    @Throws(IOException::class)
    @Anno
    fun foo()
}

annotation class Anno

// FILE: B.kt

import kotlin.reflect.jvm.javaMethod

class B(a: A) : A by a

fun box(): String {
    val method = B::foo.javaMethod!!
    if (method.exceptionTypes.size != 1)
        return "Fail throws: ${method.exceptionTypes.toList()}"

    if (method.declaredAnnotations.size != 1)
        return "Fail annotations: ${method.declaredAnnotations.toList()}"

    return "OK"
}
