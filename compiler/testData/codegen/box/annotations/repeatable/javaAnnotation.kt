// !LANGUAGE: +RepeatableAnnotations
// TARGET_BACKEND: JVM_IR
// WITH_RUNTIME
// FULL_JDK
// FILE: box.kt

import test.J
import test.Js

@J("O")
@J("K")
class Z

fun box(): String {
    val annotations = Z::class.java.annotations.filter { it.annotationClass != Metadata::class }
    if (annotations.size != 1) return "Fail 1: $annotations"

    val js = annotations.single()
    if (js !is Js) return "Fail 2: $js"

    val j = js.value.asList()
    if (j.size != 2) return "Fail 3: $j"

    return j.fold("") { acc, it -> acc + it.value }
}

// FILE: test/J.java

package test;

import java.lang.annotation.*;

@Repeatable(Js.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface J {
    String value();
}

// FILE: test/Js.java

package test;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface Js {
    J[] value();
}
