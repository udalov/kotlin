// "Create property 'foo'" "true"
// ERROR: Property must be initialized or be abstract

class A<T>(val n: T) {
    val foo: A<T>
}

fun test<U>(u: U) {
    val a: A<U> = A(u).foo
}
