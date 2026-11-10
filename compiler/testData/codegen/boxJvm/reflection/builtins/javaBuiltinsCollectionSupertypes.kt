// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: JavaListImpl.java
public class JavaListImpl extends java.util.ArrayList<String> implements KotlinJavaList {
    public JavaListImpl() {
        add("OK");
    }
}

// FILE: box.kt
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KotlinCharSequence : java.lang.CharSequence {
    override fun length(): Int = 2
    override fun charAt(index: Int): Char = "OK"[index]
    override fun subSequence(start: Int, end: Int): CharSequence = "OK".subSequence(start, end)
}

class KotlinNumber : java.lang.Number() {
    override fun byteValue(): Byte = 1
    override fun shortValue(): Short = 2
    override fun intValue(): Int = 42
    override fun longValue(): Long = 4L
    override fun floatValue(): Float = 5.0f
    override fun doubleValue(): Double = 6.0
}

interface KotlinJavaList : java.util.List<String>

private fun KClass<*>.function(name: String, parameterCount: Int): KFunction<*> =
    members.filterIsInstance<KFunction<*>>().single { it.name == name && it.parameters.size == parameterCount }

fun box(): String {
    val charSequence = KotlinCharSequence()
    assertEquals(2, KotlinCharSequence::class.function("length", 1).call(charSequence))
    assertEquals('O', KotlinCharSequence::class.function("charAt", 2).call(charSequence, 0))
    assertTrue(KotlinCharSequence::class.members.any { it.name == "subSequence" })

    val number = KotlinNumber()
    assertEquals(42, KotlinNumber::class.function("intValue", 1).call(number))

    val javaList = KotlinJavaList::class
    assertEquals(List::class, javaList.supertypes.single { it.classifier == List::class }.classifier)
    val list = JavaListImpl()
    assertEquals("OK", javaList.function("get", 2).call(list, 0))
    assertEquals(true, javaList.function("add", 2).call(list, "value"))

    return "OK"
}