package generation

import ast.Node
import ast.NodeLabel
import parser.pop
import semantic.Entry
import semantic.Local
import java.io.File
import java.io.FileWriter

class MoonGenerator (val global: HashMap<String, Entry>, val outputMoon: File) {

    init {
        FileWriter(outputMoon).use { out -> out.write("") }
    }
    val lineUp = 8
    var moonDataCode = ""
    var moonExecCode = ""
    val registerPool = ArrayDeque(REGISTERS)

    var tempVarCounter = 0
    var labelCounter = 0

    fun generate(node: Node?) {
        traverse(node, global)
        writeMoon(moonExecCode)
        writeMoon("\n$moonDataCode")
    }
    fun traverse(node: Node?, symTab: HashMap<String, Entry>) {
        if (node == null)
            return
        when (node.name) {
            NodeLabel.PROG.toString() -> {
                moonExecCode += indent()+"$ENTRY\n"
                for (child: Node in node.children) {
                    traverse(child, global)
                }
                moonExecCode += indent()+"$HALT\n"
            }
            NodeLabel.FUNCDEF.toString() -> {
                val funcHead = node.children[0]
                val funcHeadToken = funcHead.children[0].t!!
                val funcId = funcHeadToken.lexeme
                val funcBody = node.children[1]

                val innerTable = global[funcId]?.innerTable
                if (innerTable != null)
                    traverse(funcBody, innerTable)

            }
            NodeLabel.FUNCBODY.toString() -> {
                for (child: Node in node.children) {
                    traverse(child, symTab)
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
                for (child in node.children)
                    traverse(child, symTab)

                val localRegister = registerPool.removeLast()

                moonExecCode += indent()+" % assigning variable as expression\n"
                moonExecCode += indent()+"$LOAD_WORD $localRegister,${node.children[1].moonVarName}(r0)\n"
                moonExecCode += indent()+"$STORE_WORD ${node.children[0].moonVarName}(r0),$localRegister\n"

                registerPool.add(localRegister)
            }
            NodeLabel.INTLIT.toString() -> {
                node.moonVarName = getTempVar()
                val intValue = node.t!!.lexeme
                val localRegister = registerPool.removeLast()

                moonDataCode += indent()+" % space for temp variable\n"
                moonDataCode += indent(node.moonVarName)+"res 4\n"
                moonExecCode += indent()+"$ADD_I $localRegister,r0,$intValue\n"
                moonExecCode += indent()+"$STORE_WORD ${node.moonVarName}(r0),$localRegister\n"

                registerPool.add(localRegister)
            }
            NodeLabel.ADDOP.toString() -> {
                for (child in node.children)
                    traverse(child, symTab)
                node.moonVarName = getTempVar()

                val addOp = node.children[1]

                val localRegister = registerPool.removeLast()
                val leftRegister = registerPool.removeLast()
                val rightRegister = registerPool.removeLast()

                moonExecCode += indent()+" % addition\n"
                moonExecCode += indent()+"$LOAD_WORD $leftRegister,${node.children[0].moonVarName}(r0)\n"
                moonExecCode += indent()+"$LOAD_WORD $rightRegister,${node.children[2].moonVarName}(r0)\n"
                when (addOp.name) {
                    NodeLabel.PLUS.toString() -> {
                        moonExecCode += indent()+"$ADD $localRegister,$leftRegister,$rightRegister\n"
                    }
                    NodeLabel.MINUS.toString() -> {
                        moonExecCode += indent()+"$SUB $localRegister,$leftRegister,$rightRegister\n"
                    }
                    NodeLabel.OR.toString() -> {
                        println("ADDOP - OR NOT IMPLEMENTED")
                    }
                }

                moonDataCode += node.moonVarName.padEnd(lineUp)+"dw 0\n"
                moonExecCode += indent()+"$STORE_WORD ${node.moonVarName}(r0),$localRegister\n"

                registerPool.add(rightRegister)
                registerPool.add(leftRegister)
                registerPool.add(localRegister)
            }
            NodeLabel.MULTOP.toString() -> {
                for (child in node.children)
                    traverse(child, symTab)
                node.moonVarName = getTempVar()

                val multOp = node.children[1]

                val localRegister = registerPool.removeLast()
                val leftRegister = registerPool.removeLast()
                val rightRegister = registerPool.removeLast()

                moonExecCode += indent()+" % multiplication\n"
                moonExecCode += indent()+"$LOAD_WORD $leftRegister,${node.children[0].moonVarName}(r0)\n"
                moonExecCode += indent()+"$LOAD_WORD $rightRegister,${node.children[2].moonVarName}(r0)\n"

                when (multOp.name) {
                    NodeLabel.MULT.toString() -> {
                        moonExecCode += indent()+"$MUL $localRegister,$leftRegister,$rightRegister\n"
                    }
                    NodeLabel.DIV.toString() -> {
                        moonExecCode += indent()+"$DIV $localRegister,$leftRegister,$rightRegister\n"
                    }
                    NodeLabel.AND.toString() -> {
                        println("MULTOP - AND NOT IMPLEMENTED")
                    }
                }

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
                traverse(child, symTab)
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
                traverse(child, symTab)

                val localRegister = registerPool.removeLast()
                val tempR = registerPool.removeLast()

                // call putint in util.m
                // requires the int in r1, returns to r15
                moonExecCode += indent()+" % printing an int ${child.moonVarName}\n"
                moonExecCode += indent()+"$LOAD_WORD r1, ${child.moonVarName}(r0)\n"
                moonExecCode += indent()+"$JUMP_LINK r15, putint\n"

                registerPool.add(tempR)
                registerPool.add(localRegister)
            }
            NodeLabel.READ.toString() -> {
                // read should only ever have 1 child, warn if not
                if (node.children.count() > 1)
                    println("More than one child in a READ.")
                val child = node.children[0]
                traverse(child, symTab)

                val localRegister = registerPool.removeLast()

                var local = symTab[child.moonVarName]
                if (local != null) {
                    local = local as Local
                    when (local.variable.type) {
                        "integer" -> {
                            moonExecCode += indent()+" % reading an int \n"
                            moonExecCode += indent()+"$JUMP_LINK r15, getint\n"
                            moonExecCode += indent()+"$STORE_WORD ${child.moonVarName}(r0), r1\n"
                        }
                    }
                }

                registerPool.add(localRegister)
            }
            NodeLabel.IF.toString() -> {
                moonExecCode += indent("\n")+" % generating code for if branch\n"
                val relExpr = node.children[0]
                traverse(relExpr, symTab)

                val localRegister = registerPool.pop()
                val falseBlockLabel = getLabel()
                val skipFalseLabel = getLabel()
                moonExecCode += indent()+"$LOAD_WORD $localRegister, ${relExpr.moonVarName}(r0)\n"
                moonExecCode += indent()+"$BRANCH_IF_ZERO $localRegister, $falseBlockLabel\n"

                val trueStatBlock = node.children[1]
                traverse(trueStatBlock, symTab)
                moonExecCode += indent()+"$JUMP $skipFalseLabel\n"
                moonExecCode += indent(falseBlockLabel)+"\n"
                val falseStatBlock = node.children[2]
                traverse(falseStatBlock, symTab)
                moonExecCode += indent(skipFalseLabel)+"\n"

            }
            NodeLabel.RELEXPR.toString() -> {
                moonExecCode += indent()+" % generating relexpr\n"
                val lhs = node.children[0]
                val op = node.children[1]
                val rhs = node.children[2]
                traverse(lhs, symTab)
                traverse(rhs, symTab)

                val tempVar = getTempVar()
                node.moonVarName = tempVar

                moonDataCode += indent()+" % space for temp variable\n"
                moonDataCode += indent(node.moonVarName)+"res 4\n"

                val localRegister = registerPool.removeLast()
                val leftRegister = registerPool.removeLast()
                val rightRegister = registerPool.removeLast()

                // TODO other cases
                when (op.name) {
                    NodeLabel.EQ.toString() -> {

                    }
                    NodeLabel.NEQ.toString() -> {

                    }
                    NodeLabel.LT.toString() -> {

                    }
                    NodeLabel.GT.toString() -> {
                        // cgt Ri,Rj,Rk
                        moonExecCode += indent()+"$LOAD_WORD $leftRegister, ${lhs.moonVarName}(r0)\n"
                        moonExecCode += indent()+"$LOAD_WORD $rightRegister, ${rhs.moonVarName}(r0)\n"
                        moonExecCode += indent()+"cgt $localRegister,$leftRegister,$rightRegister\n"
                        moonExecCode += indent()+"$STORE_WORD ${node.moonVarName}(r0), $localRegister\n"
                    }
                    NodeLabel.LEQ.toString() -> {

                    }
                    NodeLabel.GEQ.toString() -> {

                    }
                }

                moonExecCode += indent()+"\n"
                registerPool.add(rightRegister)
                registerPool.add(leftRegister)
                registerPool.add(localRegister)
            }
            NodeLabel.STATBLOCK.toString() -> {
                for (child in node.children) {
                    traverse(child, symTab)
                }
            }
            NodeLabel.WHILE.toString() -> {
                // TODO handle loop branches
            }
        }
    }

    private fun writeMoon(s: String) {
        FileWriter(outputMoon, true).use { out -> out.write(s)}
    }

    private fun getTempVar(): String {
        return "t${tempVarCounter++}"
    }

    private fun getLabel(): String {
        return "L${labelCounter++}"
    }

    private fun indent(s: String = ""): String {
        return s.padEnd(lineUp)
    }

}
