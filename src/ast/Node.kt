package ast

import lexer.Token

data class Node(val name: String, var parent: Node? = null, var t: Token? = null) {
    val children = mutableListOf<Node>()
    var leftmostChild: Node? = null
    var leftmostSibling: Node? = null
    var rightSibling: Node? = null
    var moonVarName: String = "MOONVARNAME"

    // Adding a child node to a parent
    fun addChild(child: Node) {
        if (children.isEmpty()) {
            // parent has no children, create a pointer to the new child as leftmostChild
            leftmostChild = child
        }
        else {
            // parent has children, add the new child as a rightSibling to the rightmost sibling
            children.last().rightSibling = child
            // link the new child to its leftmostSibling, i.e. the leftmostChild of 'this' Node (parent)
            child.leftmostSibling = leftmostChild
        }

        children.add(child)
        child.parent = this
    }
}