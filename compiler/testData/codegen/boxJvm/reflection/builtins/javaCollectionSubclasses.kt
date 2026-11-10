// TARGET_BACKEND: JVM
// WITH_REFLECT
// FULL_JDK
// FILE: JavaList.java
import java.util.*;

public class JavaList extends ArrayList<String> {
    @Override
    public boolean contains(Object o) {
        return "contains".equals(o);
    }

    @Override
    public int indexOf(Object o) {
        return 42;
    }

    @Override
    public boolean remove(Object o) {
        return "remove".equals(o);
    }

    @Override
    public String remove(int index) {
        return "removeAt" + index;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return c.contains("containsAll");
    }

    @Override
    public int size() {
        return 3;
    }
}

// FILE: JavaGenericList.java
import java.util.*;

public class JavaGenericList<T> extends ArrayList<T> {
    @Override
    public boolean contains(Object o) {
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return true;
    }
}

// FILE: JavaMap.java
import java.util.*;

public class JavaMap extends HashMap<String, Integer> {
    @Override
    public Integer get(Object key) {
        return 42;
    }

    @Override
    public boolean containsKey(Object key) {
        return "containsKey".equals(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return Integer.valueOf(1).equals(value);
    }

    @Override
    public Integer remove(Object key) {
        return 239;
    }

    @Override
    public Set<String> keySet() {
        return Collections.singleton("keys");
    }
}

// FILE: box.kt
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMembers
import kotlin.test.assertEquals

class KotlinSubclassOfJavaList : JavaList()

class KotlinSubclassOfJavaMap : JavaMap() {
    override fun containsKey(key: String): Boolean = key == "Kotlin"
}

private fun KClass<*>.members(name: String): List<String> =
    members.filter { it.name == name }.map { it.toString() }.sorted()

private fun KClass<*>.member(name: String, parameterCount: Int = 2): KCallable<*> =
    members.single { it.name == name && it.parameters.size == parameterCount }

private fun KClass<*>.check(name: String, vararg expected: String) {
    assertEquals(expected.toList(), members(name))
}

fun box(): String {
    // Value parameter types of built-in functions such as `Collection.contains(E)` are erased in the corresponding JDK methods:
    // `java.util.Collection.contains(Object)`. Java methods which override them are loaded with value parameter types of built-in functions.
    for (klass in listOf(JavaList::class, KotlinSubclassOfJavaList::class)) {
        val name = klass.simpleName
        klass.check("contains", "fun $name.contains(kotlin.String!): kotlin.Boolean")
        klass.check("indexOf", "fun $name.indexOf(kotlin.String!): kotlin.Int")
        klass.check("containsAll", "fun $name.containsAll(kotlin.collections.Collection<kotlin.String!>): kotlin.Boolean")
        // `java.util.List.remove(int)` is `removeAt` in Kotlin.
        klass.check("remove", "fun $name.remove(kotlin.String!): kotlin.Boolean")
        klass.check("removeAt", "fun $name.removeAt(kotlin.Int): kotlin.String!")
        klass.check("size", "val $name.size: kotlin.Int")
        klass.check("lastIndexOf", "fun $name.lastIndexOf(kotlin.String!): kotlin.Int")

        val list = if (klass == JavaList::class) JavaList() else KotlinSubclassOfJavaList()
        assertEquals(true, klass.member("contains").call(list, "contains"))
        assertEquals(false, klass.member("contains").call(list, "remove"))
        assertEquals(42, klass.member("indexOf").call(list, ""))
        assertEquals(true, klass.member("containsAll").call(list, listOf("containsAll")))
        assertEquals(true, klass.member("remove").call(list, "remove"))
        assertEquals("removeAt1", klass.member("removeAt").call(list, 1))
        assertEquals(3, klass.member("size", 1).call(list))
        assertEquals(-1, klass.member("lastIndexOf").call(list, ""))
    }

    assertEquals(
        listOf(
            "fun JavaList.contains(kotlin.String!): kotlin.Boolean",
            "fun JavaList.containsAll(kotlin.collections.Collection<kotlin.String!>): kotlin.Boolean",
            "fun JavaList.indexOf(kotlin.String!): kotlin.Int",
            "fun JavaList.remove(kotlin.String!): kotlin.Boolean",
            "fun JavaList.removeAt(kotlin.Int): kotlin.String!",
            "val JavaList.size: kotlin.Int",
        ),
        JavaList::class.declaredMembers.map { it.toString() }.sorted(),
    )
    assertEquals(emptyList(), KotlinSubclassOfJavaList::class.declaredMembers)

    val javaList = JavaList()
    assertEquals("fun JavaList.contains(kotlin.String!): kotlin.Boolean", JavaList::contains.toString())
    assertEquals(true, JavaList::contains.call(javaList, "contains"))
    assertEquals("fun JavaList.removeAt(kotlin.Int): kotlin.String!", JavaList::removeAt.toString())
    assertEquals("removeAt2", JavaList::removeAt.call(javaList, 2))
    assertEquals(JavaList::class.member("contains"), JavaList::contains)
    assertEquals(JavaList::class.member("removeAt"), JavaList::removeAt)

    JavaGenericList::class.check("contains", "fun JavaGenericList<T>.contains(T!): kotlin.Boolean")
    JavaGenericList::class.check("removeAll", "fun JavaGenericList<T>.removeAll(kotlin.collections.Collection<T!>): kotlin.Boolean")
    JavaGenericList::class.check("indexOf", "fun JavaGenericList<T>.indexOf(T!): kotlin.Int")
    assertEquals(true, JavaGenericList::class.member("contains").call(JavaGenericList<String>(), 42))

    JavaMap::class.check("get", "fun JavaMap.get(kotlin.String!): kotlin.Int?")
    JavaMap::class.check("containsKey", "fun JavaMap.containsKey(kotlin.String!): kotlin.Boolean")
    JavaMap::class.check("containsValue", "fun JavaMap.containsValue(kotlin.Int!): kotlin.Boolean")
    JavaMap::class.check(
        "remove",
        "fun JavaMap.remove(kotlin.String!): kotlin.Int?",
        "fun JavaMap.remove(kotlin.String!, kotlin.Int!): kotlin.Boolean",
    )
    // `java.util.Map.keySet()` is `keys` in Kotlin.
    JavaMap::class.check("keys", "val JavaMap.keys: kotlin.collections.MutableSet<kotlin.String!>")
    assertEquals(emptyList(), JavaMap::class.members("keySet"))
    val map = JavaMap()
    assertEquals(42, JavaMap::class.member("get").call(map, ""))
    assertEquals(true, JavaMap::class.member("containsKey").call(map, "containsKey"))
    assertEquals(true, JavaMap::class.member("containsValue").call(map, 1))
    assertEquals(239, JavaMap::class.member("remove").call(map, ""))
    assertEquals(setOf("keys"), JavaMap::class.member("keys", 1).call(map))
    assertEquals("fun JavaMap.get(kotlin.String!): kotlin.Int?", JavaMap::get.toString())
    assertEquals(42, JavaMap::get.call(map, ""))

    KotlinSubclassOfJavaMap::class.check("containsKey", "fun KotlinSubclassOfJavaMap.containsKey(kotlin.String): kotlin.Boolean")
    KotlinSubclassOfJavaMap::class.check("containsValue", "fun KotlinSubclassOfJavaMap.containsValue(kotlin.Int!): kotlin.Boolean")
    val kotlinMap = KotlinSubclassOfJavaMap()
    assertEquals(true, KotlinSubclassOfJavaMap::containsKey.call(kotlinMap, "Kotlin"))
    assertEquals(42, KotlinSubclassOfJavaMap::class.member("get").call(kotlinMap, ""))

    return "OK"
}
