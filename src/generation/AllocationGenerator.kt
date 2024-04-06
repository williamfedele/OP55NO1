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
            NodeLabel.FUNCDEF.toString() -> {

            }
            NodeLabel.STRUCT.toString() -> {

            }
        }
    }
}
