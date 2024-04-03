package generation

import ast.Node
import semantic.Entry
import java.io.File

class MoonGenerator (val symbolTable: HashMap<String, Entry>, val outputMoon: File) {

    val registerPool = ArrayDeque(REGISTERS)

    fun generate(node: Node?) {
        if (node == null)
            return
        when (node.name) {

        }
        println("we in")
    }

}
