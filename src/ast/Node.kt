package ast

import lexer.Token

data class Node(val s: String, var parent: Node? = null, var t: Token? = null) {

    val name = s
    val children = mutableListOf<Node>()

    fun addChild(child: Node) {
        children.add(child)
        child.parent = this
    }
}