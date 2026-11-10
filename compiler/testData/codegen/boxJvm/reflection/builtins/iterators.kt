// TARGET_BACKEND: JVM
// WITH_REFLECT

// FILE: JavaIterator.java
public class JavaIterator implements java.util.Iterator<String> {
    private boolean hasNext = true;

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public String next() {
        hasNext = false;
        return "OK";
    }
}

// FILE: JavaIteratorWithRemove.java
public class JavaIteratorWithRemove extends JavaIterator {
    public boolean removed = false;

    @Override
    public void remove() {
        removed = true;
    }
}

// FILE: JavaListIterator.java
public abstract class JavaListIterator implements java.util.ListIterator<String> {
    @Override
    public void set(String s) {
    }
}

// FILE: box.kt
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KotlinIterator : Iterator<String> {
    private var hasNext = true
    override fun hasNext(): Boolean = hasNext
    override fun next(): String = "OK".also { hasNext = false }
}

class KotlinMutableIterator : MutableIterator<String> {
    private var hasNext = true
    var removed = false
    override fun hasNext(): Boolean = hasNext
    override fun next(): String = "OK".also { hasNext = false }
    override fun remove() {
        removed = true
    }
}

abstract class KotlinListIterator : ListIterator<String>

abstract class KotlinMutableListIterator : MutableListIterator<String>

class KotlinSubclassOfJava : JavaIteratorWithRemove()

private fun KClass<*>.functions(): List<String> =
    members.filterIsInstance<KFunction<*>>().map { it.toString() }.sorted()

private fun KClass<*>.function(name: String): KFunction<*> =
    members.filterIsInstance<KFunction<*>>().single { it.name == name }

private val anyFunctions = listOf("equals(kotlin.Any?): kotlin.Boolean", "hashCode(): kotlin.Int", "toString(): kotlin.String")

private fun check(klass: KClass<*>, vararg expected: String) {
    assertEquals((expected.toList() + anyFunctions).map { "fun ${klass.simpleName}.$it" }.sorted(), klass.functions())
}

fun box(): String {
    check(
        KotlinIterator::class,
        "forEachRemaining(java.util.function.Consumer<in kotlin.String>): kotlin.Unit",
        "hasNext(): kotlin.Boolean",
        "next(): kotlin.String",
    )
    // `remove` is a member of `MutableIterator` only.
    check(
        KotlinMutableIterator::class,
        "forEachRemaining(java.util.function.Consumer<in kotlin.String>): kotlin.Unit",
        "hasNext(): kotlin.Boolean",
        "next(): kotlin.String",
        "remove(): kotlin.Unit",
    )
    check(
        KotlinListIterator::class,
        "forEachRemaining(java.util.function.Consumer<in kotlin.String>): kotlin.Unit",
        "hasNext(): kotlin.Boolean",
        "hasPrevious(): kotlin.Boolean",
        "next(): kotlin.String",
        "nextIndex(): kotlin.Int",
        "previous(): kotlin.String",
        "previousIndex(): kotlin.Int",
    )
    check(
        KotlinMutableListIterator::class,
        "add(kotlin.String): kotlin.Unit",
        "forEachRemaining(java.util.function.Consumer<in kotlin.String>): kotlin.Unit",
        "hasNext(): kotlin.Boolean",
        "hasPrevious(): kotlin.Boolean",
        "next(): kotlin.String",
        "nextIndex(): kotlin.Int",
        "previous(): kotlin.String",
        "previousIndex(): kotlin.Int",
        "remove(): kotlin.Unit",
        "set(kotlin.String): kotlin.Unit",
    )

    // Java subclasses of `java.util.Iterator` inherit members from `MutableIterator`.
    check(
        JavaIterator::class,
        "forEachRemaining(java.util.function.Consumer<in kotlin.String!>): kotlin.Unit",
        "hasNext(): kotlin.Boolean",
        "next(): kotlin.String!",
        "remove(): kotlin.Unit",
    )
    check(
        JavaIteratorWithRemove::class,
        "forEachRemaining(java.util.function.Consumer<in kotlin.String!>): kotlin.Unit",
        "hasNext(): kotlin.Boolean",
        "next(): kotlin.String!",
        "remove(): kotlin.Unit",
    )
    check(
        JavaListIterator::class,
        "add(kotlin.String!): kotlin.Unit",
        "forEachRemaining(java.util.function.Consumer<in kotlin.String!>): kotlin.Unit",
        "hasNext(): kotlin.Boolean",
        "hasPrevious(): kotlin.Boolean",
        "next(): kotlin.String!",
        "nextIndex(): kotlin.Int",
        "previous(): kotlin.String!",
        "previousIndex(): kotlin.Int",
        "remove(): kotlin.Unit",
        "set(kotlin.String!): kotlin.Unit",
    )
    check(
        KotlinSubclassOfJava::class,
        "forEachRemaining(java.util.function.Consumer<in kotlin.String!>): kotlin.Unit",
        "hasNext(): kotlin.Boolean",
        "next(): kotlin.String!",
        "remove(): kotlin.Unit",
    )

    assertTrue(KotlinIterator::class.function("next").isOperator)
    assertTrue(KotlinIterator::class.function("hasNext").isOperator)
    assertTrue(JavaIterator::class.function("next").isOperator)
    assertTrue(JavaIterator::class.function("hasNext").isOperator)
    assertFalse(JavaListIterator::class.function("set").isAbstract)
    assertTrue(JavaListIterator::class.function("add").isAbstract)

    val kotlinIterator = KotlinIterator()
    assertEquals(true, KotlinIterator::hasNext.call(kotlinIterator))
    assertEquals("OK", KotlinIterator::next.call(kotlinIterator))
    assertEquals(false, KotlinIterator::class.function("hasNext").call(kotlinIterator))

    val kotlinMutableIterator = KotlinMutableIterator()
    assertEquals("OK", KotlinMutableIterator::class.function("next").call(kotlinMutableIterator))
    KotlinMutableIterator::remove.call(kotlinMutableIterator)
    assertTrue(kotlinMutableIterator.removed)

    val javaIterator = JavaIteratorWithRemove()
    assertEquals(true, JavaIteratorWithRemove::class.function("hasNext").call(javaIterator))
    assertEquals("OK", JavaIteratorWithRemove::class.function("next").call(javaIterator))
    JavaIteratorWithRemove::class.function("remove").call(javaIterator)
    assertTrue(javaIterator.removed)

    val kotlinSubclassOfJava = KotlinSubclassOfJava()
    assertEquals("OK", KotlinSubclassOfJava::next.call(kotlinSubclassOfJava))
    KotlinSubclassOfJava::class.function("remove").call(kotlinSubclassOfJava)
    assertTrue(kotlinSubclassOfJava.removed)

    return "OK"
}
