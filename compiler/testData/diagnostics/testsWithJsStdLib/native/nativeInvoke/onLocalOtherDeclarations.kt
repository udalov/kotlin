// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

fun foo() {
    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>[nativeInvoke]
    fun toplevelFun()<!> {}

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>[nativeInvoke]
    val toplevelVal<!> = 0

    [nativeInvoke]
    class <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>Foo<!> {}
}
