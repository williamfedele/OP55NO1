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
    companion object {
        // Makes a simple node using the type of the token. Ex: id, plus, minus...
        fun makeNode(t: Token, s: String): Node{
            return Node(t.type.repr.uppercase(Locale.getDefault()), null, t)
        }

        fun makeFamilyUntilNull(sstack: Stack<Node?>, parent: String): Node {
            val new = Node(parent)
            val inOrderStack = Stack<Node?>() // put nodes in order due to reversed nature of stack

            while (sstack.top() != null) {
                inOrderStack.push(sstack.pop()!!)
            }
            sstack.pop()

            while (inOrderStack.isNotEmpty()) {
                new.addChild(inOrderStack.pop()!!)
            }

            return new
        }

        // Makes the n nodes on top of the stack children of a new node created with the parent string
        fun makeFamily(sstack: Stack<Node?>, parent: String, n: Int): Node {
            val new = Node(parent)
            val inOrderStack = Stack<Node?>() // put nodes in order due to reversed nature of stack

            for (i in 1..n) {
                inOrderStack.push(sstack.pop()!!)
            }

            while (inOrderStack.isNotEmpty()) {
                new.addChild(inOrderStack.pop()!!)
            }

            return new
        }

        // Special case operation for sign to provide context for a term. Second pop contains whether its a PLUS or MINUS
        fun makeSign(sstack: Stack<Node?>): Node {
            val term = sstack.pop()!!
            val sign = sstack.pop()!!
            val new = Node(sign.t!!.type.repr.uppercase(Locale.getDefault()))
            new.addChild(term)
            return new
        }

        // Recursively prints the AST nodes using indentation to denote children
        fun astPrint(root: Node?, padding: String = "", outputFile: File) {
            if (root == null)
                return

            outputFile.appendText("$padding${root.name}\n")

            //println("$padding${root.name}")
            for (child: Node in root.children) {
                astPrint(child, "$padding| ", outputFile)
            }
        }
    }
}



