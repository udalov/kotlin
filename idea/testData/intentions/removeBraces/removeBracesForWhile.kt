fun doSomething<T>(a: T) {}

fun foo() {
    while (true) {
        doSomething("test")
    <caret>}
}
