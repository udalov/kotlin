fun test() {
    class Test()

    fun Test.plus(): Test = Test()

    val test = Test()
    test.pl<caret>us()
}
