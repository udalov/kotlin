fun foo(s: String){ }

fun getString(i: Int): String?{}

fun bar() {
    foo(<caret>)
}

// ELEMENT_TEXT: "?: getString(i: Int)"
