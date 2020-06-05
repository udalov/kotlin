#!/usr/bin/env kotlin

import java.io.*

val String.indent: Int
    get() {
        var ans = 0
        while (ans < length && this[ans] == ' ') ans++
        return ans
    }

fun List<String>.findLine(text: String): List<Int> {
    return withIndex().filter { (_, line) -> text in line }.map { it.index }
}

fun List<String>.findFun(signature: String): List<Pair<Int, Int>> {
    return findLine(signature).map { begin ->
        val line = this[begin].trimEnd()
        var end = begin + 1
        if (line.endsWith("=")) {
            while (this[end].indent > 4) end++
        } else if (line.endsWith("{")) {
            while (this[end] != "    }") end++
            end++
        }
        Pair(begin, end)
    }.reversed()
}

fun String.replaceAssert(old: String, new: String): String {
    require(old in this) { "String: $this\nOld:$old\nNew:$new" }
    return replace(old, new)
}

fun processFile(file: File) {
    val originalLines = file.readLines()
    val lines = originalLines.toMutableList()

    val acceptSignature = "override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R"
    val acceptVoidSignature = "override fun acceptVoid(visitor: IrElementVisitorVoid)"
    for ((i, j) in lines.findFun(acceptSignature)) {
        val new = lines.subList(i, j).toMutableList()
        new.add("")
        new[0] = new[0].replaceAssert(acceptSignature, acceptVoidSignature)
        for (k in 0 until new.size) {
            new[k] = new[k].replace(", data)", ")")
        }
        lines.addAll(i, new)
    }

    val acceptChildrenSignature = "override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D)"
    val acceptChildrenVoidSignature = "override fun acceptChildrenVoid(visitor: IrElementVisitorVoid)"
    for ((i, j) in lines.findFun(acceptChildrenSignature)) {
        val new = lines.subList(i, j).toMutableList()
        new.add("")
        new[0] = new[0].replaceAssert(acceptChildrenSignature, acceptChildrenVoidSignature)
        for (k in 0 until new.size) {
            new[k] = new[k].replace(", data)", ")").replace("accept(", "acceptVoid(").replace("acceptChildren(", "acceptChildrenVoid(")
        }
        lines.addAll(i, new)
    }

    if (lines != originalLines) {
        val importVisitorVoid = "import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid"
        if (lines.findLine(importVisitorVoid).isEmpty()) {
            val i = lines.findLine("import org.jetbrains.kotlin.ir.visitors.IrElementVisitor").firstOrNull()
            if (i != null) lines.add(i, importVisitorVoid)
        }
    }

    if (lines != originalLines) {
        println("${originalLines.size} -> ${lines.size}: $file")
        file.writeText(lines.joinToString("\n"))
    }
}

for (file in File("/Users/udalov/kotlin/compiler/ir").walkTopDown()) {
    if (!file.isFile || file.extension != "kt") continue
    try {
        processFile(file)
    } catch (e: Throwable) {
        throw AssertionError("Error on $file:\n${e.message}", e)
    }
}
