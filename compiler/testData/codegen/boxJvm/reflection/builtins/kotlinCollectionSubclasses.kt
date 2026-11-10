// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KotlinIterable : Iterable<String> {
    override fun iterator(): Iterator<String> = listOf("OK").iterator()
}

class KotlinList : List<String> by listOf("OK")

class KotlinMutableList : MutableList<String> by mutableListOf("OK")

class KotlinSet : Set<String> by setOf("OK")

class KotlinMap : Map<String, String> by mapOf("answer" to "OK")

class KotlinMutableMap : MutableMap<String, String> by mutableMapOf("answer" to "OK")

private fun KClass<*>.member(name: String, parameterCount: Int = 1): KCallable<*> =
    members.single { it.name == name && it.parameters.size == parameterCount }

private fun KClass<*>.check(expected: String, name: String, parameterCount: Int = 1) {
    assertEquals(expected, member(name, parameterCount).toString())
}

private fun KClass<*>.hasMember(name: String): Boolean =
    members.any { it.name == name }

fun box(): String {
    // Built-in read-only collection classes do not have members of mutable collections.
    assertTrue(List::class.hasMember("size"))
    assertTrue(List::class.hasMember("contains"))
    assertTrue(List::class.hasMember("spliterator"))
    assertFalse(List::class.hasMember("add"))
    assertFalse(List::class.hasMember("removeAt"))
    assertFalse(List::class.hasMember("removeIf"))
    assertTrue(Map::class.hasMember("keys"))
    assertFalse(Map::class.hasMember("keySet"))
    assertFalse(Map::class.hasMember("put"))

    KotlinIterable::class.check("fun KotlinIterable.iterator(): kotlin.collections.Iterator<kotlin.String>", "iterator")
    KotlinIterable::class.check("fun KotlinIterable.spliterator(): java.util.Spliterator<kotlin.String>", "spliterator")
    assertEquals("OK", KotlinIterable::iterator.call(KotlinIterable()).next())

    KotlinList::class.check("val KotlinList.size: kotlin.Int", "size")
    KotlinList::class.check("fun KotlinList.get(kotlin.Int): kotlin.String", "get", 2)
    KotlinList::class.check("fun KotlinList.contains(kotlin.String): kotlin.Boolean", "contains", 2)
    KotlinList::class.check("fun KotlinList.spliterator(): java.util.Spliterator<kotlin.String!>", "spliterator")
    assertFalse(KotlinList::class.hasMember("add"))
    assertFalse(KotlinList::class.hasMember("removeAt"))
    assertFalse(KotlinList::class.hasMember("removeIf"))
    val list = KotlinList()
    assertEquals(1, KotlinList::size.get(list))
    assertEquals("OK", KotlinList::get.call(list, 0))
    assertEquals(true, KotlinList::class.member("contains", 2).call(list, "OK"))

    KotlinMutableList::class.check("val KotlinMutableList.size: kotlin.Int", "size")
    KotlinMutableList::class.check("fun KotlinMutableList.contains(kotlin.String): kotlin.Boolean", "contains", 2)
    KotlinMutableList::class.check("fun KotlinMutableList.removeAt(kotlin.Int): kotlin.String", "removeAt", 2)
    KotlinMutableList::class.check("fun KotlinMutableList.remove(kotlin.String): kotlin.Boolean", "remove", 2)
    KotlinMutableList::class.check(
        "fun KotlinMutableList.removeIf(java.util.function.Predicate<in kotlin.String>): kotlin.Boolean", "removeIf", 2,
    )
    KotlinMutableList::class.check(
        "fun KotlinMutableList.replaceAll(java.util.function.UnaryOperator<kotlin.String>): kotlin.Unit", "replaceAll", 2,
    )
    KotlinMutableList::class.check("fun KotlinMutableList.spliterator(): java.util.Spliterator<kotlin.String!>", "spliterator")
    val mutableList = KotlinMutableList()
    assertEquals(1, KotlinMutableList::size.get(mutableList))
    assertEquals(true, KotlinMutableList::class.member("add", 2).call(mutableList, "A"))
    assertEquals("OK", KotlinMutableList::removeAt.call(mutableList, 0))
    assertEquals(true, KotlinMutableList::class.member("remove", 2).call(mutableList, "A"))
    assertEquals(0, mutableList.size)

    KotlinSet::class.check("val KotlinSet.size: kotlin.Int", "size")
    KotlinSet::class.check("fun KotlinSet.contains(kotlin.String): kotlin.Boolean", "contains", 2)
    assertFalse(KotlinSet::class.hasMember("add"))
    assertEquals(true, KotlinSet::contains.call(KotlinSet(), "OK"))

    KotlinMap::class.check("val KotlinMap.size: kotlin.Int", "size")
    KotlinMap::class.check("val KotlinMap.keys: kotlin.collections.Set<kotlin.String>", "keys")
    KotlinMap::class.check(
        "val KotlinMap.entries: kotlin.collections.Set<kotlin.collections.Map.Entry<kotlin.String, kotlin.String>>", "entries",
    )
    KotlinMap::class.check("fun KotlinMap.get(kotlin.String): kotlin.String?", "get", 2)
    KotlinMap::class.check("fun KotlinMap.containsKey(kotlin.String): kotlin.Boolean", "containsKey", 2)
    assertFalse(KotlinMap::class.hasMember("keySet"))
    assertFalse(KotlinMap::class.hasMember("entrySet"))
    assertFalse(KotlinMap::class.hasMember("put"))
    val map = KotlinMap()
    assertEquals(1, KotlinMap::size.get(map))
    assertEquals(setOf("answer"), KotlinMap::keys.get(map))
    assertEquals("OK", KotlinMap::get.call(map, "answer"))

    KotlinMutableMap::class.check("val KotlinMutableMap.keys: kotlin.collections.MutableSet<kotlin.String>", "keys")
    KotlinMutableMap::class.check(
        "val KotlinMutableMap.entries: " +
                "kotlin.collections.MutableSet<kotlin.collections.MutableMap.MutableEntry<kotlin.String, kotlin.String>>",
        "entries",
    )
    KotlinMutableMap::class.check("fun KotlinMutableMap.put(kotlin.String, kotlin.String): kotlin.String?", "put", 3)
    KotlinMutableMap::class.check("fun KotlinMutableMap.remove(kotlin.String): kotlin.String?", "remove", 2)
    assertFalse(KotlinMutableMap::class.hasMember("keySet"))
    val mutableMap = KotlinMutableMap()
    assertEquals(null, KotlinMutableMap::put.call(mutableMap, "key", "value"))
    assertEquals("value", KotlinMutableMap::get.call(mutableMap, "key"))
    assertEquals("OK", KotlinMutableMap::class.member("remove", 2).call(mutableMap, "answer"))
    assertEquals(1, KotlinMutableMap::size.get(mutableMap))

    return "OK"
}
