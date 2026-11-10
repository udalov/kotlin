// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KotlinCharSequence : CharSequence {
    override val length: Int = 2

    override fun get(index: Int): Char = "OK"[index]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = "OK".subSequence(startIndex, endIndex)
}

class KotlinNumber : Number() {
    override fun toByte(): Byte = 1
    override fun toChar(): Char = '1'
    override fun toDouble(): Double = 1.0
    override fun toFloat(): Float = 1.0f
    override fun toInt(): Int = 1
    override fun toLong(): Long = 1L
    override fun toShort(): Short = 1
}

fun box(): String {
    val charSequence = KotlinCharSequence()
    val charSequenceMembers = KotlinCharSequence::class.members
    assertTrue(charSequenceMembers.any { it.name == "get" })
    assertTrue(charSequenceMembers.any { it.name == "length" })
    assertTrue(charSequenceMembers.any { it.name == "subSequence" })
    assertEquals('O', KotlinCharSequence::get.call(charSequence, 0))
    assertEquals(2, KotlinCharSequence::length.get(charSequence))

    val number = KotlinNumber()
    assertTrue(KotlinNumber::class.members.any { it.name == "toInt" })
    assertEquals(1, KotlinNumber::toInt.call(number))

    return "OK"
}