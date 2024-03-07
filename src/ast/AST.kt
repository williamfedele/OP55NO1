package ast

import lexer.Token
import parser.Stack
import parser.pop
import parser.top
import java.io.File
import java.util.*


class AST {
    companion object {
        fun makeNode(t: Token, s: String): Node{
            return Node(t.type.repr.uppercase(Locale.getDefault()), null, t)
        }
        // TODO: give children nodes connections to right sibling and first child.
        // give parent connection to first child.
        fun makeFamilyUntilNull(sstack: Stack<Node?>, parent: String): Node {
            val new = Node(parent)
            while (sstack.top() != null) {
                new.addChild(sstack.pop()!!)
            }
            sstack.pop()
            return new
        }

        fun makeFamily(sstack: Stack<Node?>, parent: String, n: Int): Node {
            val new = Node(parent)
            for (i in 1..n) {
                new.addChild(sstack.pop()!!)
            }
            return new
        }

        fun makeSign(sstack: Stack<Node?>): Node {
            val factor = sstack.pop()!!
            val sign = sstack.pop()!!
            val new = Node(sign.t!!.type.repr.uppercase(Locale.getDefault()))
            new.addChild(factor)
            return new
        }

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


