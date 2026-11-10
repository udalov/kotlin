// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: D.java
public abstract class D {
    public abstract String getX();

    public String getY() {
        return "D.y";
    }

    public String getZ() {
        return "D.z";
    }

    public void setZ(String z) {
    }

    public String getW() {
        return "D.w";
    }
}

// FILE: J1.java
public class J1 extends D implements K {
    @Override
    public String getX() {
        return "J1.x";
    }

    @Override
    public String getY() {
        return "J1.y";
    }
}

// FILE: J2.java
public abstract class J2 extends D implements K {
}

// FILE: box.kt
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

interface K {
    val x: String
    val y: String
    var z: String
}

class J3 : J2() {
    override val x: String get() = "J3.x"
}

private fun KClass<*>.checkMembers(vararg expected: String) {
    val names = setOf("x", "y", "z", "getX", "getY", "getZ", "setZ", "getW")
    assertEquals(expected.toList(), members.filter { it.name in names }.map { it.toString() }.sorted())
}

private fun KClass<*>.property(name: String): KProperty1<Any, *> =
    members.single { it.name == name } as KProperty1<Any, *>

fun box(): String {
    // `J1.getX` and `J1.getY` override getters of `K.x` and `K.y`, so both these methods and the methods `getX` and `getY` inherited from `D`
    // are not visible as functions.
    // `D.getZ` and `D.setZ` override accessors of `K.z` in `J1`.
    J1::class.checkMembers(
        "fun J1.getW(): kotlin.String!",
        "val J1.x: kotlin.String",
        "val J1.y: kotlin.String",
        "var J1.z: kotlin.String",
    )
    val j1 = J1()
    assertEquals("J1.x", J1::class.property("x").get(j1))
    assertEquals("J1.y", J1::class.property("y").get(j1))
    assertEquals("D.z", J1::class.property("z").get(j1))
    assertFalse(J1::class.property("x").isAbstract)
    assertFalse(J1::class.property("z").isAbstract)
    assertTrue(J1::class.property("z") is KMutableProperty1<*, *>)

    // There are no methods in `J2`, but methods inherited from `D` override accessors of properties inherited from `K`.
    J2::class.checkMembers(
        "fun J2.getW(): kotlin.String!",
        "val J2.x: kotlin.String",
        "val J2.y: kotlin.String",
        "var J2.z: kotlin.String",
    )
    assertTrue(J2::class.property("x").isAbstract)
    assertFalse(J2::class.property("y").isAbstract)
    assertFalse(J2::class.property("z").isAbstract)

    J3::class.checkMembers(
        "fun J3.getW(): kotlin.String!",
        "val J3.x: kotlin.String",
        "val J3.y: kotlin.String",
        "var J3.z: kotlin.String",
    )
    val j3 = J3()
    assertEquals("J3.x", J3::class.property("x").get(j3))
    assertEquals("D.y", J3::class.property("y").get(j3))
    assertEquals("D.z", J3::class.property("z").get(j3))
    assertFalse(J3::class.property("x").isAbstract)
    assertFalse(J3::class.property("y").isAbstract)

    return "OK"
}
