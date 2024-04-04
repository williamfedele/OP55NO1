package generation

import ast.Node
import ast.NodeLabel
import semantic.Entry
import java.io.File
import java.io.FileWriter

class MoonGenerator (val symbolTable: HashMap<String, Entry>, val outputMoon: File) {

    init {
        FileWriter(outputMoon).use { out -> out.write("") }
    }
    val lineUp = 8
    var moonDataCode = ""
    var moonExecCode = ""
    val registerPool = ArrayDeque(REGISTERS)

    var tempVarCounter = 0

    fun generate(node: Node?) {
        traverse(node)

        moonExecCode += "putint   add    r2,r0,r0         % c := 0 (character)\n" +
                "         add    r3,r0,r0         % s := 0 (sign)\n" +
                "         addi   r4,r0,endbuf     % p is the buffer pointer\n" +
                "         cge    r5,r1,r0\n" +
                "         bnz    r5,putint1       % branch if n >= 0\n" +
                "         addi   r3,r0,1          % s := 1\n" +
                "         sub    r1,r0,r1         % n := -n\n" +
                "putint1  modi   r2,r1,10         % c := n mod 10\n" +
                "         addi   r2,r2,48         % c := c + '0'\n" +
                "         subi   r4,r4,1          % p := p - 1\n" +
                "         sb     0(r4),r2         % buf[p] := c\n" +
                "         divi   r1,r1,10         % n := n div 10\n" +
                "         bnz    r1,putint1       % do next digit\n" +
                "         bz     r3,putint2       % branch if n >= 0\n" +
                "         addi   r2,r0,45         % c := '-'\n" +
                "         subi   r4,r4,1          % p := p - 1\n" +
                "         sb     0(r4),r2         % buf[p] := c\n" +
                "putint2  lb     r2,0(r4)         % c := buf[p]\n" +
                "         putc   r2               % write c\n" +
                "         addi   r4,r4,1          % p := p + 1\n" +
                "         cgei   r5,r4,endbuf\n" +
                "         bz     r5,putint2       % branch if more digits\n" +
                "         jr     r15              % return\n" +
                "\n" +
                "         res    20               % digit buffer\n" +
                "endbuf\n"
        writeMoon(moonExecCode)
        writeMoon("\n$moonDataCode")
    }
    fun traverse(node: Node?) {
        if (node == null)
            return
        when (node.name) {
            NodeLabel.PROG.toString() -> {
                moonExecCode += indent()+"entry\n"
                for (child: Node in node.children) {
                    traverse(child)
                }
                moonDataCode += indent()+"% buffer space used for console output\n"
                moonDataCode += indent("buf")+"res 20\n"
                moonExecCode += indent()+"hlt\n"
            }
            NodeLabel.FUNCDEF.toString() -> {
                val funcHead = node.children[0]
                val funcHeadToken = funcHead.children[0].t!!
                val funcId = funcHeadToken.lexeme
                val funcBody = node.children[1]
                if (funcId == "main") {
                    traverse(funcBody)
                }
            }
            NodeLabel.FUNCBODY.toString() -> {
                for (child: Node in node.children) {
                    traverse(child)
                }
            }
            NodeLabel.VARDECL.toString() -> {
                val varIdToken = node.children[0].t!!
                val varId = varIdToken.lexeme
                val varType = node.children[1].t!!.lexeme
                val dimList = node.children[2].children

                when (varType) {
                    "integer" -> {
                        var multDim = 1
                        for (str in dimList) {
                            try {
                                val parsedInt = str.t!!.lexeme.toInt()
                                multDim *= parsedInt
                            } catch (e: NumberFormatException) {
                                // not a valid int
                            }
                        }
                        moonDataCode += indent()+" % space for variable $varId\n"
                        moonDataCode += indent(varId)+"res ${multDim*4}\n"
                    }
                }
            }
            NodeLabel.ASSIGNSTAT.toString() -> {
                // child 1 gets the value of child 2
                // child2 = addop
                // addop adds child 1 and child 2
                // child 1 is int
                // child 2 is arithexpr is multop
                // multop multiplies child 1 and child 2
                // both child 1 and child 2 are int
                for (child in node.children)
                    traverse(child)

                val localRegister = registerPool.removeLast()

                moonExecCode += indent()+" % assigning variable as expression\n"
                moonExecCode += indent()+"$LOAD_WORD $localRegister,${node.children[1].moonVarName}(r0)\n"
                moonExecCode += indent()+"$STORE_WORD ${node.children[0].moonVarName}(r0),$localRegister\n"

                registerPool.add(localRegister)
            }
            NodeLabel.INTLIT.toString() -> {
                node.moonVarName = getTempVar()
                moonDataCode += indent()+" % space for temp variable\n"
                moonDataCode += indent(node.moonVarName)+"res 4\n"
            }
            NodeLabel.ADDOP.toString() -> {
                for (child in node.children)
                    traverse(child)
                node.moonVarName = getTempVar()

                val localRegister = registerPool.removeLast()
                val leftRegister = registerPool.removeLast()
                val rightRegister = registerPool.removeLast()

                moonExecCode += indent()+" % addition\n"
                moonExecCode += indent()+"$LOAD_WORD $leftRegister,${node.children[0].moonVarName}(r0)\n"
                moonExecCode += indent()+"$LOAD_WORD $rightRegister,${node.children[1].moonVarName}(r0)\n"
                moonExecCode += indent()+"$ADD $localRegister,$leftRegister,$rightRegister\n"
                moonDataCode += node.moonVarName.padEnd(lineUp)+"dw 0\n"
                moonExecCode += indent()+"$STORE_WORD ${node.moonVarName}(r0),$localRegister\n"

                registerPool.add(rightRegister)
                registerPool.add(leftRegister)
                registerPool.add(localRegister)
            }
            NodeLabel.MULTOP.toString() -> {
                for (child in node.children)
                    traverse(child)
                node.moonVarName = getTempVar()

                val localRegister = registerPool.removeLast()
                val leftRegister = registerPool.removeLast()
                val rightRegister = registerPool.removeLast()

                moonExecCode += indent()+" % multiplication\n"
                moonExecCode += indent()+"$LOAD_WORD $leftRegister,${node.children[0].moonVarName}(r0)\n"
                moonExecCode += indent()+"$LOAD_WORD $rightRegister,${node.children[1].moonVarName}(r0)\n"
                moonExecCode += indent()+"$MUL $localRegister,$leftRegister,$rightRegister\n"
                moonDataCode += node.moonVarName.padEnd(lineUp)+"dw 0\n"
                moonExecCode += indent()+"$STORE_WORD ${node.moonVarName}(r0),$localRegister\n"

                registerPool.add(rightRegister)
                registerPool.add(leftRegister)
                registerPool.add(localRegister)
            }
            NodeLabel.ARITHEXPR.toString() -> {
                // arithexpr should only ever have 1 child, warn if not
                if (node.children.count() > 1)
                    println("More than one child in an ARITHEXPR.")
                val child = node.children[0]
                traverse(child)
                node.moonVarName = child.moonVarName
            }
            NodeLabel.VARIABLE.toString() -> {
                val varId = node.children[0].t!!.lexeme
                val varIndiceList = node.children[1]

                node.moonVarName = varId
            }
            NodeLabel.WRITE.toString() -> {
                // write should only ever have 1 child, warn if not
                if (node.children.count() > 1)
                    println("More than one child in a WRITE.")
                val child = node.children[0]
                traverse(child)

                val localRegister = registerPool.removeLast()
                val tempR = registerPool.removeLast()
                /*
                moonExecCode += indent()+" % printing ${child.moonVarName}\n"
                moonExecCode += indent()+"$LOAD_WORD $localRegister,${child.moonVarName}(r0)\n"
                moonExecCode += indent()+"% put value on stack\n"
                moonExecCode += indent()+"$STORE_WORD -8(r14),$localRegister\n"
                moonExecCode += indent()+"% link buffer to stack\n"
                moonExecCode += indent()+"$ADD_I $localRegister,r0,buf\n"
                moonExecCode += indent()+"$STORE_WORD -12(r14),$localRegister\n"
                moonExecCode += indent()+"% convert int to string for output\n"
                moonExecCode += indent()+"$JUMP_LINK r15, intstr\n"
                moonExecCode += indent()+"$STORE_WORD -8(r14),r13\n"
                moonExecCode += indent()+"% output to console\n"
                moonExecCode += indent()+"$JUMP_LINK r15, putstr\n"
                */
                moonExecCode += indent()+"$LOAD_WORD r1, ${child.moonVarName}(r0)\n"
                moonExecCode += indent()+"$JUMP_LINK r15, putint\n"
                // TODO this is outputting 0. figure out why!
                registerPool.add(tempR)
                registerPool.add(localRegister)
            }
        }
    }

    private fun writeMoon(s: String) {
        FileWriter(outputMoon, true).use { out -> out.write(s)}
    }

    private fun getTempVar(): String {
        return "t${tempVarCounter++}"
    }

    private fun indent(s: String = ""): String {
        return s.padEnd(lineUp)
    }

}
