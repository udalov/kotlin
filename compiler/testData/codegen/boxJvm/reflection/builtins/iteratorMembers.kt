// TARGET_BACKEND: JVM
// WITH_REFLECT
// FULL_JDK

// FILE: JavaIterator.java

public class JavaIterator implements java.util.Iterator<String> {
    @Override
    public boolean hasNext() { return false; }

    @Override
    public String next() { return ""; }
}

// FILE: test.kt

import kotlin.reflect.KClass
import kotlin.test.assertEquals

interface MyIterator<T> : Iterator<T>

interface MyMutableIterator<T> : MutableIterator<T>

interface MyMutableListIterator<T> : MutableListIterator<T>

class MyListIterator : MutableListIterator<String> {
    override fun hasNext(): Boolean = false
    override fun next(): String = ""
    override fun hasPrevious(): Boolean = false
    override fun previous(): String = ""
    override fun nextIndex(): Int = 0
    override fun previousIndex(): Int = 0
    override fun remove() {}
    override fun set(element: String) {}
    override fun add(element: String) {}
}

fun check(klass: KClass<*>, vararg expected: String) {
    assertEquals(expected.toList(), klass.members.map(Any::toString).sorted())
}

fun box(): String {
    check(
        Iterator::class,
        "fun kotlin.collections.Iterator<T>.equals(kotlin.Any?): kotlin.Boolean",
        "fun kotlin.collections.Iterator<T>.forEachRemaining(java.util.function.Consumer<in T>): kotlin.Unit",
        "fun kotlin.collections.Iterator<T>.hasNext(): kotlin.Boolean",
        "fun kotlin.collections.Iterator<T>.hashCode(): kotlin.Int",
        "fun kotlin.collections.Iterator<T>.next(): T",
        "fun kotlin.collections.Iterator<T>.toString(): kotlin.String",
    )
    check(
        ListIterator::class,
        "fun kotlin.collections.ListIterator<T>.equals(kotlin.Any?): kotlin.Boolean",
        "fun kotlin.collections.ListIterator<T>.forEachRemaining(java.util.function.Consumer<in T>): kotlin.Unit",
        "fun kotlin.collections.ListIterator<T>.hasNext(): kotlin.Boolean",
        "fun kotlin.collections.ListIterator<T>.hasPrevious(): kotlin.Boolean",
        "fun kotlin.collections.ListIterator<T>.hashCode(): kotlin.Int",
        "fun kotlin.collections.ListIterator<T>.next(): T",
        "fun kotlin.collections.ListIterator<T>.nextIndex(): kotlin.Int",
        "fun kotlin.collections.ListIterator<T>.previous(): T",
        "fun kotlin.collections.ListIterator<T>.previousIndex(): kotlin.Int",
        "fun kotlin.collections.ListIterator<T>.toString(): kotlin.String",
    )
    check(
        MyIterator::class,
        "fun MyIterator<T>.equals(kotlin.Any?): kotlin.Boolean",
        "fun MyIterator<T>.forEachRemaining(java.util.function.Consumer<in T>): kotlin.Unit",
        "fun MyIterator<T>.hasNext(): kotlin.Boolean",
        "fun MyIterator<T>.hashCode(): kotlin.Int",
        "fun MyIterator<T>.next(): T",
        "fun MyIterator<T>.toString(): kotlin.String",
    )
    check(
        MyMutableIterator::class,
        "fun MyMutableIterator<T>.equals(kotlin.Any?): kotlin.Boolean",
        "fun MyMutableIterator<T>.forEachRemaining(java.util.function.Consumer<in T>): kotlin.Unit",
        "fun MyMutableIterator<T>.hasNext(): kotlin.Boolean",
        "fun MyMutableIterator<T>.hashCode(): kotlin.Int",
        "fun MyMutableIterator<T>.next(): T",
        "fun MyMutableIterator<T>.remove(): kotlin.Unit",
        "fun MyMutableIterator<T>.toString(): kotlin.String",
    )
    check(
        MyMutableListIterator::class,
        "fun MyMutableListIterator<T>.add(T): kotlin.Unit",
        "fun MyMutableListIterator<T>.equals(kotlin.Any?): kotlin.Boolean",
        "fun MyMutableListIterator<T>.forEachRemaining(java.util.function.Consumer<in T>): kotlin.Unit",
        "fun MyMutableListIterator<T>.hasNext(): kotlin.Boolean",
        "fun MyMutableListIterator<T>.hasPrevious(): kotlin.Boolean",
        "fun MyMutableListIterator<T>.hashCode(): kotlin.Int",
        "fun MyMutableListIterator<T>.next(): T",
        "fun MyMutableListIterator<T>.nextIndex(): kotlin.Int",
        "fun MyMutableListIterator<T>.previous(): T",
        "fun MyMutableListIterator<T>.previousIndex(): kotlin.Int",
        "fun MyMutableListIterator<T>.remove(): kotlin.Unit",
        "fun MyMutableListIterator<T>.set(T): kotlin.Unit",
        "fun MyMutableListIterator<T>.toString(): kotlin.String",
    )
    check(
        MyListIterator::class,
        "fun MyListIterator.add(kotlin.String): kotlin.Unit",
        "fun MyListIterator.equals(kotlin.Any?): kotlin.Boolean",
        "fun MyListIterator.forEachRemaining(java.util.function.Consumer<in kotlin.String>): kotlin.Unit",
        "fun MyListIterator.hasNext(): kotlin.Boolean",
        "fun MyListIterator.hasPrevious(): kotlin.Boolean",
        "fun MyListIterator.hashCode(): kotlin.Int",
        "fun MyListIterator.next(): kotlin.String",
        "fun MyListIterator.nextIndex(): kotlin.Int",
        "fun MyListIterator.previous(): kotlin.String",
        "fun MyListIterator.previousIndex(): kotlin.Int",
        "fun MyListIterator.remove(): kotlin.Unit",
        "fun MyListIterator.set(kotlin.String): kotlin.Unit",
        "fun MyListIterator.toString(): kotlin.String",
    )
    check(
        JavaIterator::class,
        "fun JavaIterator.equals(kotlin.Any?): kotlin.Boolean",
        "fun JavaIterator.forEachRemaining(java.util.function.Consumer<in kotlin.String!>): kotlin.Unit",
        "fun JavaIterator.hasNext(): kotlin.Boolean",
        "fun JavaIterator.hashCode(): kotlin.Int",
        "fun JavaIterator.next(): kotlin.String",
        "fun JavaIterator.remove(): kotlin.Unit",
        "fun JavaIterator.toString(): kotlin.String",
    )
    check(
        java.util.PrimitiveIterator.OfInt::class,
        "fun java.util.PrimitiveIterator.OfInt.equals(kotlin.Any?): kotlin.Boolean",
        "fun java.util.PrimitiveIterator.OfInt.forEachRemaining(java.util.function.Consumer<in kotlin.Int!>): kotlin.Unit",
        "fun java.util.PrimitiveIterator.OfInt.forEachRemaining(java.util.function.IntConsumer!): kotlin.Unit",
        "fun java.util.PrimitiveIterator.OfInt.hasNext(): kotlin.Boolean",
        "fun java.util.PrimitiveIterator.OfInt.hashCode(): kotlin.Int",
        "fun java.util.PrimitiveIterator.OfInt.next(): kotlin.Int!",
        "fun java.util.PrimitiveIterator.OfInt.nextInt(): kotlin.Int",
        "fun java.util.PrimitiveIterator.OfInt.remove(): kotlin.Unit",
        "fun java.util.PrimitiveIterator.OfInt.toString(): kotlin.String",
    )

    return "OK"
}
