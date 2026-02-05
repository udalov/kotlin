inline fun <reified T> Any?.foo() = this as T

fun bar(x: Any?) = x.foo<String>()

// 0 NEW java/lang/NullPointerException
// 1 INVOKESTATIC kotlin/jvm/internal/Intrinsics.checkNotNull
