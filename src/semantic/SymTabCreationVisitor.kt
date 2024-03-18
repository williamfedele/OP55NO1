package semantic

import ast.Node
import ast.Semantic

class SymTabCreationVisitor: Visitor {
    override fun visit(node: Node) {
        when (node.name) {
            Semantic.Companion.NodeLabel.PROG.str -> {
                node.symTab = SymTab("global")
                for (child: Node in node.children)
                    child.accept(this)
            }
        }
    }
}