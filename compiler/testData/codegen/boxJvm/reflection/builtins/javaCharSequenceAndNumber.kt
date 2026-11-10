// TARGET_BACKEND: JVM
// WITH_REFLECT

// FILE: JavaCharSequence.java
public final class JavaCharSequence implements CharSequence {
    @Override
    public int length() {
        return 2;
    }

    @Override
    public char charAt(int index) {
        return "OK".charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return "OK".subSequence(start, end);
    }

    @Override
    public String toString() {
        return "OK";
    }
}

// FILE: JavaNumber.java
public final class JavaNumber extends Number {
    @Override
    public byte byteValue() {
        return 1;
    }

    @Override
    public short shortValue() {
        return 1;
    }

    @Override
    public int intValue() {
        return 1;
    }

    @Override
    public long longValue() {
        return 1L;
    }

    @Override
    public float floatValue() {
        return 1.0f;
    }

    @Override
    public double doubleValue() {
        return 1.0;
    }
}

// FILE: JavaCharSequenceWithKotlinSuperClass.java
public final class JavaCharSequenceWithKotlinSuperClass extends KotlinAbstractCharSequence {
    @Override
    public int getLength() {
        return 2;
    }

    @Override
    public char get(int index) {
        return "OK".charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return "OK".subSequence(start, end);
    }
}

// FILE: box.kt
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class KotlinAbstractCharSequence : CharSequence

fun box(): String {
    val charSequence = JavaCharSequence()
    val charSequenceMembers = JavaCharSequence::class.members
    assertTrue(charSequenceMembers.any { it.name == "get" })
    assertTrue(charSequenceMembers.any { it.name == "length" })
    assertTrue(charSequenceMembers.any { it.name == "subSequence" })
    assertEquals('O', charSequenceMembers.single { it.name == "get" }.call(charSequence, 0))
    val lengthMembers = charSequenceMembers.filter { it.name == "length" }
    assertEquals(1, lengthMembers.size, lengthMembers.joinToString())
    assertEquals(2, lengthMembers.single().call(charSequence))

    // If a Java class has a Kotlin superclass which inherits the builtin, it overrides builtin members by their Kotlin names.
    val charSequenceWithKotlinSuperClass = JavaCharSequenceWithKotlinSuperClass()
    val charSequenceWithKotlinSuperClassMembers = JavaCharSequenceWithKotlinSuperClass::class.members
    assertEquals('O', charSequenceWithKotlinSuperClassMembers.single { it.name == "get" }.call(charSequenceWithKotlinSuperClass, 0))
    assertEquals(2, charSequenceWithKotlinSuperClassMembers.single { it.name == "length" }.call(charSequenceWithKotlinSuperClass))
    assertTrue(charSequenceWithKotlinSuperClassMembers.none { it.name == "getLength" || it.name == "charAt" })

    val number = JavaNumber()
    val numberMembers = JavaNumber::class.members
    assertTrue(numberMembers.any { it.name == "toInt" })
    assertEquals(1, numberMembers.single { it.name == "toInt" }.call(number))
    assertTrue(numberMembers.none { it.name == "intValue" })
    assertTrue(charSequenceMembers.none { it.name == "charAt" })

    return "OK"
}