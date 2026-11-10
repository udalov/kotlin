// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

interface I {
    val x: String
    var y: String
}

open class C {
    open val x: String get() = "OK"
    open var y: String = ""
}

open class F {
    val x: String get() = "OK"
}

abstract class A {
    abstract val x: String
}

interface J : I {
    override val x: String get() = "OK"
}

abstract class D1 : C(), I
abstract class D2 : I, C()
abstract class D3 : F(), I
abstract class D4 : A(), I
abstract class D5 : I, J

private fun KClass<*>.member(name: String): KCallable<*> = members.single { it.name == name }

private fun KClass<*>.checkModality(name: String, isAbstract: Boolean, isOpen: Boolean, isFinal: Boolean) {
    val member = member(name)
    assertEquals(isAbstract, member.isAbstract, "isAbstract of $member")
    assertEquals(isOpen, member.isOpen, "isOpen of $member")
    assertEquals(isFinal, member.isFinal, "isFinal of $member")
}

fun box(): String {
    // Modality of a property inherited from several supertypes doesn't depend on the order of supertypes.
    D1::class.checkModality("x", isAbstract = false, isOpen = true, isFinal = false)
    D1::class.checkModality("y", isAbstract = false, isOpen = true, isFinal = false)
    D2::class.checkModality("x", isAbstract = false, isOpen = true, isFinal = false)
    D2::class.checkModality("y", isAbstract = false, isOpen = true, isFinal = false)

    D3::class.checkModality("x", isAbstract = false, isOpen = false, isFinal = true)
    D4::class.checkModality("x", isAbstract = true, isOpen = false, isFinal = false)

    // `J.x` overrides `I.x`, so the abstract `I.x` doesn't affect the modality.
    D5::class.checkModality("x", isAbstract = false, isOpen = true, isFinal = false)

    return "OK"
}
