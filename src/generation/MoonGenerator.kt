package generation

import ast.Node
import ast.NodeLabel
import semantic.Entry
import java.io.File
import java.io.FileWriter

class MoonGenerator (val symbolTable: HashMap<String, Entry>, val outputMoon: File) {

    init {
        FileWriter(outputMoon).use { out -> out.write("    % label-based code") }
    }

    val registerPool = ArrayDeque(REGISTERS)

    fun generate(node: Node?) {
        if (node == null)
            return
        when (node.name) {
            NodeLabel.PROG.toString() -> {
                for (child: Node in node.children) {
                    generate(child)
                }
            }
            NodeLabel.FUNCDEF.toString() -> {
                val funcHead = node.children[0]
                val funcHeadToken = funcHead.children[0].t!!
                val funcId = funcHeadToken.lexeme
                val funcBody = node.children[1]
                if (funcId == "main") {
                    generate(funcBody)
                }
            }
            NodeLabel.FUNCBODY.toString() -> {
                for (child: Node in node.children) {
                    when (child.name) {
                        NodeLabel.VARDECL.toString() -> {
                            generate(child)
                        }
                    }
                }
            }
            NodeLabel.VARDECL.toString() -> {
                val varIdToken = node.children[0].t!!
                val varId = varIdToken.lexeme
                val varType = node.children[1].t!!.lexeme
                val dimList = node.children[2].children

                when (varType) {
                    "integer" -> {
                        if (dimList.isEmpty()) {
                            writeMoon("    % space for variable $varId")
                            writeMoon("$varId   res 4")
                        }
                        else {
                            var multDim = 1
                            for (str in dimList) {
                                try {
                                    val parsedInt = str.t!!.lexeme.toInt()
                                    multDim *= parsedInt
                                } catch (e: NumberFormatException) {
                                    // not a valid int
                                }
                            }
                            writeMoon("    % space for variable $varId")
                            writeMoon("$varId   res ${multDim*4}")
                        }
                    }
                }
            }
        }
    }

    private fun writeMoon(s: String) {
        FileWriter(outputMoon, true).use { out -> out.write("$s\n")}
    }

}
