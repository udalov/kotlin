// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: J.java
import java.util.List;

public class J implements A, B<String> {
    @Override
    public String getX() {
        return "OK";
    }

    @Override
    public List<String> getY() {
        return null;
    }
}

// FILE: box.kt
import kotlin.test.assertEquals

interface A {
    val x: String?
    val y: List<String>?
}

interface B<T> {
    val x: T & Any
    val y: MutableList<T & Any>
}

fun box(): String {
    val members = J::class.members

    // The type of a property in a Java class is enhanced according to types of all properties it overrides, not only the first one.
    assertEquals("val J.x: kotlin.String", members.single { it.name == "x" }.toString())
    assertEquals("val J.y: kotlin.collections.MutableList<kotlin.String>", members.single { it.name == "y" }.toString())

    return "OK"
}
