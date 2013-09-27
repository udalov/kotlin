// "Suppress 'REDUNDANT_NULLABLE' for var (a, b)" "true"

fun foo() {
    [suppress("REDUNDANT_NULLABLE")]
    var (a, b) = Pair<String?<caret>?, String>("", "")
}

data class Pair<A, B>(val a: A, val b: B)