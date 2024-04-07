package generation

import ast.Node
import ast.NodeLabel
import semantic.Entry

class AllocationGenerator (val global: HashMap<String, Entry>) {


    fun generate(node: Node?) {
        traverse(node, global)
    }

    fun traverse(node: Node?, symTab: HashMap<String, Entry>) {
        if (node == null)
            return
        when (node.name) {
            NodeLabel.PROG.toString() -> {
                for (child in node.children) {
                    traverse(child, global)
                }

            }
            NodeLabel.STRUCT.toString() -> {
                for (child in node.children)
                    traverse(child, symTab)
            }
            NodeLabel.FUNCDEF.toString() -> {
                for (child in node.children)
                    traverse(child, symTab)
            }
            NodeLabel.FUNCHEAD.toString() -> {
                println("funchead")
                val fparams = node.children[1]
                traverse(fparams, symTab)
                val returnVal = node.children[2]
            }
            NodeLabel.FPARAMS.toString() -> {
                for (child in node.children)
                    traverse(child, symTab)
            }
            NodeLabel.FPARAM.toString() -> {

            }
            NodeLabel.FUNCBODY.toString() -> {
                println("funcbody")
            }

        }
    }
}
