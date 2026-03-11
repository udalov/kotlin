import org.jetbrains.kotlin.plugin.sandbox.GenerateInitializerNestedClass

@GenerateInitializerNestedClass
class A {
    private val x: String? = null
    private var y: String? = null

    override fun toString(): String = "x=$x y=$y"
}

fun box(): String {
    val a = A()
    val init = A::class.java.declaredClasses.single { it.simpleName == "Initializer" }.declaredMethods.single { it.name == "init" }
    init(null, a)
    return if (a.toString() == "x=null y=y") "OK" else "Fail: $a"
}
