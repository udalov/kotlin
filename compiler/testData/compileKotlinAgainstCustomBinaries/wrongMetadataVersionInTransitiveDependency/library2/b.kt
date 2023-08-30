package b

import a.A

fun function(param: A?): String = param.toString()

fun A?.extensionFunction() {}
var A?.extensionProperty: String
    get() = ""
    set(_) {}

class B : A()
