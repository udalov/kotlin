// SHOULD_FAIL_WITH: duplicate.or.missing.arguments
// ERROR: An argument is already passed for this parameter
// ERROR: No value passed for parameter b
fun test() {
    class Test{
        fun get(a: Int, b: Int) : Int = 0
    }
    val test = Test()
    test.g<caret>et(a=0, a=1)
}
