// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE

private class Private

private inline fun <reified T> parameterized(): String {
    if (T::class == Private::class) return "OK"
    return T::class.simpleName ?: "Unknown type"
}

internal inline fun inlineFun() = <!PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>parameterized<<!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>Private<!>>()<!>
