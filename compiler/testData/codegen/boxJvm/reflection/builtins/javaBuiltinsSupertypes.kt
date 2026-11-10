// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KFunction
import kotlin.test.assertEquals
import kotlin.test.assertTrue

interface JavaMapEntry : java.util.Map.Entry<String, Int>

class MapEntryImpl : JavaMapEntry {
    override fun getKey(): String = "OK"
    override fun getValue(): Int = 42
    override fun setValue(value: Int): Int = value
}

fun box(): String {
    val supertype = JavaMapEntry::class.supertypes.single { it.classifier == Map.Entry::class }
    assertEquals(Map.Entry::class, supertype.classifier)

    val getKey = JavaMapEntry::class.members.filterIsInstance<KFunction<*>>().singleOrNull { it.name == "getKey" }
    assertTrue(getKey != null, JavaMapEntry::class.members.joinToString("\n"))
    assertEquals("OK", getKey.call(MapEntryImpl()))

    return "OK"
}