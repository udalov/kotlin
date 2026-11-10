// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KFunction
import kotlin.test.assertEquals

interface JavaMapEntry : java.util.Map.Entry<String, Int>

class MapEntryImpl : JavaMapEntry {
    override fun getKey(): String = "OK"
    override fun getValue(): Int = 42
    override fun setValue(value: Int): Int = value
}

fun box(): String {
    val supertype = JavaMapEntry::class.supertypes.single { it.classifier == Map.Entry::class }
    assertEquals(Map.Entry::class, supertype.classifier)

    val setValue = JavaMapEntry::class.members.filterIsInstance<KFunction<*>>().single { it.name == "setValue" }
    assertEquals(7, setValue.call(MapEntryImpl(), 7))

    return "OK"
}