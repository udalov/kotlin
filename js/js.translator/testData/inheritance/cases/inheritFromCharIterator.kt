package foo

class MyCharIterator : CharIterator() {
    val data = array('O', 'K')
    var i = 0

    override fun hasNext(): Boolean = i < data.size
    override fun nextChar(): Char = data[i++]
}

fun box(): String {
    var r = ""

    for (v in MyCharIterator()) {
        r += v
    }

    return r
}