package test

import java.io.*

class C {
    @Throws(IOException::class)
    constructor()

    @Throws(Exception::class)
    fun method() {}

    @get:Throws(Exception::class)
    @set:Throws(Exception::class)
    var property = 42
}
