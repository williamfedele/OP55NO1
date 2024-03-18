package semantic

import ast.Node

interface Visitor {
    fun visit(node: Node)
}