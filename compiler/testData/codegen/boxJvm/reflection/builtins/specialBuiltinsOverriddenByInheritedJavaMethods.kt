// TARGET_BACKEND: JVM
// WITH_REFLECT
// FULL_JDK
// FILE: Base.java
public class Base {
    public boolean contains(Object o) {
        return "Base".equals(o);
    }

    public int size() {
        return 1;
    }

    public String remove(int index) {
        return "Base" + index;
    }
}

// FILE: CharSequenceBase.java
public class CharSequenceBase {
    public char charAt(int index) {
        return 'B';
    }

    public int length() {
        return 4;
    }
}

// FILE: JavaCollection.java
import java.util.*;

public class JavaCollection extends Base implements Collection<String> {
    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Iterator<String> iterator() {
        return null;
    }

    @Override
    public Object[] toArray() {
        return null;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return null;
    }

    @Override
    public boolean add(String s) {
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends String> c) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {
    }
}

// FILE: JavaList.java
public abstract class JavaList extends Base implements java.util.List<String> {
}

// FILE: JavaCharSequence.java
public class JavaCharSequence extends CharSequenceBase implements CharSequence {
    @Override
    public CharSequence subSequence(int start, int end) {
        return null;
    }
}

// FILE: box.kt
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

abstract class KotlinSubclassOfJavaList : JavaList()

class KotlinSubclassOfJavaCharSequence : JavaCharSequence()

private fun KClass<*>.check(vararg expected: String) {
    val names = setOf("contains", "size", "remove", "removeAt", "charAt", "get", "length")
    assertEquals(
        expected.map { it.replace("#", simpleName!!) },
        members.filter { it.name in names }.map { (if (it.isAbstract) "abstract " else "") + it }.sorted(),
    )
}

private fun KClass<*>.member(name: String): KCallable<*> =
    members.single { it.name == name }

fun box(): String {
    // `Base` and `CharSequenceBase` are not subclasses of any built-in class. However, their methods override special built-in functions
    // and properties inherited from another supertype in their subclasses: the ones with a different JVM name, and the ones with erased
    // value parameters.

    // `Base.contains(Object)` overrides `Collection.contains(E)`, and `Base.size()` overrides the getter of `Collection.size`.
    JavaCollection::class.check(
        "fun #.contains(kotlin.String!): kotlin.Boolean",
        "fun #.remove(kotlin.Int): kotlin.String!",
        "fun #.remove(kotlin.String!): kotlin.Boolean",
        "val #.size: kotlin.Int",
    )
    val collection = JavaCollection()
    assertEquals(true, JavaCollection::class.member("contains").call(collection, "Base"))
    assertEquals(false, JavaCollection::class.member("contains").call(collection, ""))
    assertEquals(1, JavaCollection::class.member("size").call(collection))
    assertEquals(true, JavaCollection::contains.call(collection, "Base"))

    // `Base.remove(int)` overrides `MutableList.removeAt(Int)`.
    for (klass in listOf(JavaList::class, KotlinSubclassOfJavaList::class)) {
        klass.check(
            "abstract fun #.get(kotlin.Int): kotlin.String!",
            "abstract fun #.remove(kotlin.String!): kotlin.Boolean",
            "fun #.contains(kotlin.String!): kotlin.Boolean",
            "fun #.removeAt(kotlin.Int): kotlin.String!",
            "val #.size: kotlin.Int",
        )
    }

    // `CharSequenceBase.charAt(int)` overrides `CharSequence.get(Int)`, and `CharSequenceBase.length()` overrides the getter of
    // `CharSequence.length`.
    for (klass in listOf(JavaCharSequence::class, KotlinSubclassOfJavaCharSequence::class)) {
        klass.check(
            "fun #.get(kotlin.Int): kotlin.Char",
            "val #.length: kotlin.Int",
        )
        // Calling `get` is not checked here because it fails in the K1-based implementation with "Could not compute caller for function".
        val charSequence = if (klass == JavaCharSequence::class) JavaCharSequence() else KotlinSubclassOfJavaCharSequence()
        assertEquals(4, klass.member("length").call(charSequence))
        assertTrue((klass.member("get") as KFunction<*>).isOperator)
    }
    assertEquals(4, KotlinSubclassOfJavaCharSequence::length.get(KotlinSubclassOfJavaCharSequence()))
    assertFalse(JavaCharSequence::class.member("get").isAbstract)

    return "OK"
}
