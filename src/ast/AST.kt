package ast

import lexer.Token
import parser.Stack
import parser.pop
import parser.push
import parser.top
import java.io.File
import java.util.*


/**
 * Contains operations for manipulating a semantic stack
 */
class AST {

    private val semanticStack = Stack<Node?>()

    // Makes a simple node using the type of the token. Ex: id, plus, minus...
    fun makeNode(t: Token) {
        semanticStack.push(Node(t.type.repr.uppercase(Locale.getDefault()), null, t))
    }

    // Makes nodes (until null is found) from the top of the stack children of a new node created with the parent string
    fun makeFamilyUntilNull(parent: String) {
        val new = Node(parent)
        val inOrderStack = Stack<Node?>() // put nodes in order due to reversed nature of stack

        while (semanticStack.top() != null) {
            inOrderStack.push(semanticStack.pop()!!)
        }
        semanticStack.pop()

        while (inOrderStack.isNotEmpty()) {
            new.addChild(inOrderStack.pop()!!)
        }

        semanticStack.push(new)
    }

    // Makes the n nodes on top of the stack children of a new node created with the parent string
    fun makeFamily(parent: String, n: Int) {
        val new = Node(parent)
        val inOrderStack = Stack<Node?>() // put nodes in order due to reversed nature of stack

        for (i in 1..n) {
            inOrderStack.push(semanticStack.pop()!!)
        }

        while (inOrderStack.isNotEmpty()) {
            new.addChild(inOrderStack.pop()!!)
        }

        semanticStack.push(new)
    }

    // Push null for usage with makeFamilyUntilNull
    fun makeNull() {
        semanticStack.push(null)
    }

    // To explicitly declare an empty dimlist for array function parameters.
    fun makeEmpty() {
        semanticStack.push(Node("EMPTY"))
    }

    // Special case operation for sign to provide context for a term. Second pop contains whether its a PLUS or MINUS
    fun makeSign() {
        val term = semanticStack.pop()!!
        val sign = semanticStack.pop()!!
        val new = Node(sign.t!!.type.repr.uppercase(Locale.getDefault()), null, sign.t)
        new.addChild(term)
        semanticStack.push(new)
    }

    // Recursively prints the AST nodes using indentation to denote children
    fun astPrint(outputFile: File) {
        astPrintRecur(semanticStack.top(), "", outputFile)
    }
    private fun astPrintRecur(root: Node?, padding: String = "", outputFile: File) {
        if (root == null)
            return

        outputFile.appendText("$padding${root.name}\n")

        //println("$padding${root.name}")
        for (child: Node in root.children) {
            astPrintRecur(child, "$padding| ", outputFile)
        }
    }
}



