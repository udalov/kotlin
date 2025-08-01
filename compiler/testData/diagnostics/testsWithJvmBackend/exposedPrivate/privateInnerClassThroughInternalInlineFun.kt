// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE

class A {
    private inner class Inner {
        fun foo() = "OK"
    }

    private inline fun privateFun() = Inner().foo()
    internal inline fun internalInlineFun() = <!PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>privateFun()<!>
}
