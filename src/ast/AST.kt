package ast

import lexer.Token
import parser.Stack
import parser.pop
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
        // TODO: give children nodes connections to their right sibling and the first child.
        // give parent connection to first child.
        fun makeFamilyUntilNull(sstack: Stack<Node?>, parent: String): Node {
            val new = Node(parent)
            while (sstack.top() != null) {
                new.addChild(sstack.pop()!!)
            }
            sstack.pop()
            return new
        }

        // Makes the n nodes on top of the stack children of a new node created with the parent string
        fun makeFamily(sstack: Stack<Node?>, parent: String, n: Int): Node {
            val new = Node(parent)
            for (i in 1..n) {
                new.addChild(sstack.pop()!!)
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
            for (child: Node in root.children.reversed()) {
                astPrint(child, "$padding| ", outputFile)
            }
        }
    }
}



