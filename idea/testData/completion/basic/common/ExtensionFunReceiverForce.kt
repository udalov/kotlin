class Test {
    val St<caret>
}

// INVOCATION_COUNT: 2
// EXIST: String~(jet)
// EXIST: IllegalStateException
// EXIST: StringBuilder
// EXIST_JAVA_ONLY: StringBuffer
// EXIST_JS_ONLY: HTMLStyleElement
// EXIST_JAVA_ONLY: Statement@Statement~(java.sql)
