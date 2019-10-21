// FILE: A.kt

open class A {
    private val o = object {
        fun getO(): String = "O"
    }

    private fun k() = run {
        class L {
            fun getK(): String = "K"
        }
        L()
    }

    fun result(): String = o.getO() + k().getK()
}

// FILE: B.kt

fun box(): String {
    val p = object : A() {}
    return p.result()
}
