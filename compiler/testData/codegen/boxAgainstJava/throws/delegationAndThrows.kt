// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: A.java

import java.io.IOException;

public interface A {
    void foo() throws IOException;
}

// FILE: B.kt

import kotlin.reflect.jvm.javaMethod

class B(a: A) : A by a

fun box(): String {
    val method = B::foo.javaMethod!!
    if (method.exceptionTypes.size != 1)
        return "Fail: ${method.exceptionTypes.toList()}"

    return "OK"
}
