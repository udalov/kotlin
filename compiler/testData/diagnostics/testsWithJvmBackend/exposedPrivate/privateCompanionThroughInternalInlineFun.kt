// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE

class A {
    private companion object {
        fun foo() = "OK"
    }

    private inline fun privateFun() = foo()
    internal inline fun internalInlineFun() = <!PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>privateFun()<!>
}
