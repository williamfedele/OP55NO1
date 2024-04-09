package generation

import ast.Node
import ast.NodeLabel
import semantic.Entry
import semantic.Local
import semantic.Param
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
    private fun traverse(node: Node?, symTab: HashMap<String, Entry>) {
        if (node == null)
            return
        when (node.name) {
            /**
             * PROG node contains STRUCT, IMPL or FUNCDEF
             */
            NodeLabel.PROG.toString() -> {
                // carriage return used for printing
                moonDataCode += indent()+"% carriage return\n"
                moonDataCode += indent("cr")+"db 13,10\n"
                moonDataCode += indent()+"align\n"

                for (child: Node in node.children) {
                    moonExecCode += indent()+"% new scope\n"
                    traverse(child, global)
                }
                moonExecCode += indent()+"$HALT\n"
            }
            NodeLabel.FUNCDEF.toString() -> {
                val funcHead = node.children[0]
                val funcRet = funcHead.children[2].name
                val funcHeadToken = funcHead.children[0].t!!
                val funcId = funcHeadToken.lexeme
                val funcBody = node.children[1]

                // get the function from the symbol table and assign a label for calling later
                val fn = symTab[funcId] as semantic.Function
                fn.moonLabel = getLabel()+"\n"
                moonExecCode += fn.moonLabel

                // set return temp var
                // void return type doesn't get assigned a tempvar
                if (funcRet != "VOID") {
                    fn.moonReturnLabel = getTempVar()+"\n"
                    when (funcRet) {
                        "INTEGER" -> {
                            moonDataCode += indent(fn.moonReturnLabel)+"res $INT_SIZE\n"
                        }
                        "FLOAT" -> {
                            println("float return variable not allocated")
                        }
                        "ID" -> {
                            println("id return variable not allocated")
                        }
                    }
                }

                traverse(funcHead, fn.innerTable!!)

                // moon execution should start with the main function
                if (funcId == "main")
                    moonExecCode += indent("\n")+" $ENTRY\n"

                // handle all statements declared inside the function
                // provide the symbol table of the related function
                // if this is called from a free function, symTab = global
                // if called from a impl function, symTab = the classes symtab
                val innerTable = symTab[funcId]?.innerTable
                if (innerTable != null)
                    traverse(funcBody, innerTable)

                // if the function is not main, we need to return to the caller
                // r14 has been assigned my return register
                if (funcId != "main")
                    moonExecCode += indent()+"$JUMP_REGISTER r14\n"

            }
            /**
             * return value is handled in funcdef, only need to handle parameters here
             */
            NodeLabel.FUNCHEAD.toString() -> {
                val fparams = node.children[1]
                traverse(fparams, symTab)

            }
            NodeLabel.FPARAMS.toString() -> {
                for (child in node.children)
                    traverse(child, symTab)
            }
            /**
             * reserve space for the functions input parameter
             */
            NodeLabel.FPARAM.toString() -> {
                val paramName = node.children[0].t!!.lexeme
                val paramType = node.children[1].t!!.lexeme
                val paramDimList = node.children[2].children

                // get a temp var for the parameter and save it in the symbol table
                val paramMoonVar = getTempVar()
                val paramSym = symTab[paramName] as Param
                paramSym.moonVarName = paramMoonVar

                // calculate the amount of allocations to make in the case of arrays
                var multDim = processDimList(paramDimList)

                // reserve space for the parameter temp variable
                when (paramType) {
                    "integer" -> {
                        moonDataCode += indent(paramMoonVar)+"res ${multDim * INT_SIZE}\n"
                    }
                    "float" -> {
                        moonDataCode += indent(paramMoonVar)+"res ${multDim * FLOAT_SIZE}\n"
                    }
                    else -> {
                        println("unhandle case in VARDECL: $paramName")
                    }
                }

            }
            /**
             * funcbody can have vardecl or statements
             */
            NodeLabel.FUNCBODY.toString() -> {
                for (child: Node in node.children) {
                    traverse(child, symTab)
                }
            }
            /**
             * vardecl has an ID, type, and dimlist.
             * reserve space using the ID as a label.
             */
            NodeLabel.VARDECL.toString() -> {
                val varIdToken = node.children[0].t!!
                val varId = varIdToken.lexeme
                val varType = node.children[1].t!!.lexeme
                val dimList = node.children[2].children

                moonDataCode += indent()+"% space for variable $varId\n"

                // calculate the amount of allocations to make in the case of arrays

                val multDim = processDimList(dimList)

                when (varType) {
                    "integer" -> {
                        moonDataCode += indent(varId)+"res ${multDim * INT_SIZE}\n"
                    }
                    "float" -> {
                        moonDataCode += indent(varId)+"res ${multDim * FLOAT_SIZE}\n"
                    }
                    else -> {
                        println("unhandle case in VARDECL: $varType")
                    }
                }
            }
            /**
             *
             */
            NodeLabel.ASSIGNSTAT.toString() -> {
                val lhs = node.children[0]
                traverse(lhs, symTab)
                val rhs = node.children[1]

                traverse(rhs, symTab)

                val localRegister = registerPool.removeLast()
                val offsetRegister = registerPool.removeLast()

                moonExecCode += indent()+"% resetting registers\n"
                moonExecCode += indent()+"$SUB $localRegister,$localRegister,$localRegister\n"
                moonExecCode += indent()+"$SUB $offsetRegister,$offsetRegister,$offsetRegister\n"

                moonExecCode += indent("\n")+" % assignstat\n"
                // load offset in case we're assigning to an array index
                // if offset is a temp variable, load it
                if (lhs.moonOffsetLocation == "r0") {
                    moonExecCode += indent()+"$ADD_I $offsetRegister,r0,0\n"
                }
                else {
                    moonExecCode += indent()+"$LOAD_WORD $offsetRegister,${lhs.moonOffsetLocation}(r0)\n"
                }
                //moonExecCode += indent()+"$ADD $offsetRegister,r0,${lhs.moonOffset}\n"
                // load the rhs expression result
                moonExecCode += indent()+"$LOAD_WORD $localRegister,${rhs.moonVarName}(r0)\n"
                // assign lhs + offset (if array) = rhs
                moonExecCode += indent()+"$STORE_WORD ${lhs.moonVarName}($offsetRegister),$localRegister\n"

                registerPool.add(offsetRegister)
                registerPool.add(localRegister)
            }
            /**
             * literal ints are stored as temporary variables
             */
            NodeLabel.INTLIT.toString() -> {
                node.moonVarName = getTempVar()
                val intValue = node.t!!.lexeme
                val localRegister = registerPool.removeLast()

                moonExecCode += indent()+"% resetting registers\n"
                moonExecCode += indent()+"$SUB $localRegister,$localRegister,$localRegister\n"

                moonDataCode += indent()+"% space for temp int literal\n"
                moonDataCode += indent(node.moonVarName)+"res 4\n"
                moonExecCode += indent("\n")+" % putting int literal $intValue in tempvar ${node.moonVarName}\n"
                moonExecCode += indent()+"$ADD_I $localRegister,r0,$intValue\n"
                moonExecCode += indent()+"$STORE_WORD ${node.moonVarName}(r0),$localRegister\n"

                registerPool.add(localRegister)
            }
            /* WIP
            NodeLabel.FLOATLIT.toString() -> {
                node.moonVarName = getTempVar()
                val float = node.t!!.lexeme
                val expSplit = float.split("e") // [5.3, +5]

                val intValue = (expSplit[0].toDouble() * 1000).toInt()

                val localRegister = registerPool.removeLast()

                moonDataCode += indent()+"% space for temp variable\n"
                moonDataCode += indent(node.moonVarName)+"res $INT_SIZE\n"
                moonExecCode += indent()+"$ADD_I $localRegister,r0,$intValue\n"
                moonExecCode += indent()+"$STORE_WORD ${node.moonVarName}(r0),$localRegister\n"

                if (expSplit.size > 1) {

                    moonDataCode += indent()+"% space for temp variable exponent\n"
                    moonDataCode += indent(node.moonVarName)+"res $INT_SIZE\n"
                    moonExecCode += indent()+"$ADD_I $localRegister,r0,${expSplit[1]}\n"
                    moonExecCode += indent()+"$STORE_WORD ${node.moonVarName}(r0),$localRegister\n"
                }

                registerPool.add(localRegister)

            }*/
            /**
             *
             */
            NodeLabel.ADDOP.toString() -> {
                for (child in node.children)
                    traverse(child, symTab)
                node.moonVarName = getTempVar()

                val addOp = node.children[1]

                val localRegister = registerPool.removeLast()
                val leftRegister = registerPool.removeLast()
                val rightRegister = registerPool.removeLast()

                moonExecCode += indent("\n")+" % addop\n"
                moonExecCode += indent()+"$LOAD_WORD $leftRegister,${node.children[0].moonVarName}(r0)\n"
                moonExecCode += indent()+"$LOAD_WORD $rightRegister,${node.children[2].moonVarName}(r0)\n"

                // pick the appropriate instruction for the type of add op
                when (addOp.name) {
                    NodeLabel.PLUS.toString() -> {
                        moonExecCode += indent()+"$ADD $localRegister,$leftRegister,$rightRegister\n"
                    }
                    NodeLabel.MINUS.toString() -> {
                        moonExecCode += indent()+"$SUB $localRegister,$leftRegister,$rightRegister\n"
                    }
                    NodeLabel.OR.toString() -> {
                        moonExecCode += indent()+"$OR $localRegister,$leftRegister,$rightRegister\n"
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

                moonExecCode += indent("\n")+" % multop\n"
                moonExecCode += indent()+"$LOAD_WORD $leftRegister,${node.children[0].moonVarName}(r0)\n"
                moonExecCode += indent()+"$LOAD_WORD $rightRegister,${node.children[2].moonVarName}(r0)\n"

                // pick the appropriate instruction for the type of mult op
                when (multOp.name) {
                    NodeLabel.MULT.toString() -> {
                        moonExecCode += indent()+"$MUL $localRegister,$leftRegister,$rightRegister\n"
                    }
                    NodeLabel.DIV.toString() -> {
                        moonExecCode += indent()+"$DIV $localRegister,$leftRegister,$rightRegister\n"
                    }
                    NodeLabel.AND.toString() -> {
                        moonExecCode += indent()+"$AND $localRegister,$leftRegister,$rightRegister\n"
                    }
                }

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
                val varIndiceList = node.children[1].children


                if (varIndiceList.isNotEmpty()) {
                    for (child in varIndiceList) {
                        if (child.name != NodeLabel.INTLIT.toString())
                            traverse(child, symTab)
                    }

                    moonExecCode += indent("\n")+" % setting up array indices\n"

                    val localRegister = registerPool.removeLast()
                    val accumulatorRegister = registerPool.removeLast()
                    val offsetRegister = registerPool.removeLast()

                    moonExecCode += indent()+"% resetting registers\n"
                    moonExecCode += indent()+"$SUB $localRegister,$localRegister,$localRegister\n"
                    moonExecCode += indent()+"$SUB $accumulatorRegister,$accumulatorRegister,$accumulatorRegister\n"
                    moonExecCode += indent()+"$SUB $offsetRegister,$offsetRegister,$offsetRegister\n"

                    val localEntry = symTab[varId] as Local
                    val arrShape = localEntry.variable.dim

                    // n-dimensional arrays are stored in contiguous memory
                    // calculate the stride to ensure we skip over dimensions as we index the array
                    // Ex: for a [2][4][3] array:
                    //      first dimension changes require stepping over 12 (4*3) integers (48 bytes)
                    //      second dimension changes require stepping over 3 integers (12 bytes)
                    //      third dimension changes require stepping over 1 integer (4 bytes)
                    val strides = MutableList(arrShape.size){1}
                    for (i in 0..<arrShape.size) {
                        for (j in i+1..<arrShape.size) {
                            strides[i] *= arrShape[j].toInt()
                        }
                    }

                    /**
                     * save ints in temporary variables and add it to the accumulator
                     * load the value from variables and add it to the accumulator
                     */

                    node.moonOffsetLocation = getTempVar()
                    for (i in 0..<varIndiceList.size) {
                        when (varIndiceList[i].name) {
                            NodeLabel.INTLIT.toString() -> {
                                val intValue = varIndiceList[i].t!!.lexeme.toInt()
                                if (localEntry.variable.type == "integer") {
                                    val offset = intValue * INT_SIZE * strides[i]
                                    moonExecCode += indent()+"$ADD_I $accumulatorRegister,$accumulatorRegister,$offset\n"
                                }

                            }
                            else -> {
                                // add offset of variable
                                // if offset is a temp variable, load it
                                moonExecCode += indent()+"% variable indice detected\n"
                                if (varIndiceList[i].moonOffsetLocation == "r0") {
                                    moonExecCode += indent()+"$ADD $offsetRegister,r0,${varIndiceList[i].moonOffsetLocation}\n"
                                } else {
                                    moonExecCode += indent()+"$LOAD_WORD $offsetRegister,${varIndiceList[i].moonOffsetLocation},(r0)\n"
                                }

                                // load the variable with offset applied
                                moonExecCode += indent()+"$LOAD_WORD $localRegister,${varIndiceList[i].moonVarName}($offsetRegister)\n"
                                // multiply the index value by the type size (int = 4 bytes for example)
                                moonExecCode += indent()+"$MUL_I $localRegister,$localRegister,4\n"
                                // multiply by the stride since an index will have different offsets depending on its position
                                // ex: a 2x2 integer matrix, the difference between [0][0] and [0][1] is 4 bytes
                                //      the difference between [0][0] and [1][0] is 8 bytes.
                                moonExecCode += indent()+"$MUL_I $localRegister,$localRegister,${strides[i]}\n"
                                // add to the accumulator register
                                moonExecCode += indent()+"$ADD $accumulatorRegister,$accumulatorRegister,${localRegister}\n"
                            }
                        }
                    }
                    // store the accumulator for the composite indice list in a temporary variable

                    moonDataCode += indent()+"% space for temp indice\n"
                    moonDataCode += indent(node.moonOffsetLocation)+"res 4\n"
                    moonExecCode += indent()+"$STORE_WORD ${node.moonOffsetLocation}(r0),$accumulatorRegister\n"

                    registerPool.add(offsetRegister)
                    registerPool.add(accumulatorRegister)
                    registerPool.add(localRegister)
                }
                if (node.moonVarName == ""){
                    // if the variable being used is a parameter, link it to the parameter moon tempvar
                    if (symTab.containsKey(varId) && symTab[varId] is Param)
                        node.moonVarName = (symTab[varId] as Param).moonVarName
                    else
                        node.moonVarName = varId // locally declared variable
                }

            }
            NodeLabel.WRITE.toString() -> {
                // write should only ever have 1 child, warn if not
                if (node.children.count() > 1)
                    println("More than one child in a WRITE.")
                val child = node.children[0]
                traverse(child, symTab)

                moonExecCode += indent("\n")+" % printing an int ${child.moonVarName}\n"

                val localRegister = registerPool.removeLast()
                val offsetRegister = registerPool.removeLast()

                moonExecCode += indent()+"% resetting registers\n"
                moonExecCode += indent()+"$SUB $localRegister,$localRegister,$localRegister\n"
                moonExecCode += indent()+"$SUB $offsetRegister,$offsetRegister,$offsetRegister\n"

                // call putint in util.m
                // requires the int in r1, returns to r15
                // load offset
                if (child.moonOffsetLocation == "r0") {
                    moonExecCode += indent()+"$ADD_I $localRegister,r0,0\n"
                }
                else {
                    moonExecCode += indent()+"$LOAD_WORD $localRegister, ${child.moonOffsetLocation}(r0)\n"
                }
                moonExecCode += indent()+"$ADD $offsetRegister,r0,$localRegister\n"
                moonExecCode += indent()+"$LOAD_WORD r1, ${child.moonVarName}($offsetRegister)\n"
                moonExecCode += indent()+"$JUMP_LINK r15, putint\n"
                /*
                if (node.floatExpMoonVarName == "unset") {

                }
                else {
                    // float value, convert back to decimal and combine with exponent tempvar
                    moonExecCode += indent("\n")+" % printing an int ${child.moonVarName}\n"

                }
                */

                // output a newline
                moonExecCode += indent()+"$ADD_I r1,r0,cr\n"
                moonExecCode += indent()+"$JUMP_LINK r15, putstr\n"
                registerPool.add(offsetRegister)
            }
            NodeLabel.READ.toString() -> {
                // read should only ever have 1 child, warn if not
                if (node.children.count() > 1)
                    println("More than one child in a READ.")
                val child = node.children[0]
                traverse(child, symTab)

                var local = symTab[child.moonVarName]
                if (local != null) {
                    local = local as Local
                    when (local.variable.type) {
                        "integer" -> {
                            moonExecCode += indent("\n")+" % reading an int \n"
                            moonExecCode += indent()+"$JUMP_LINK r15, getint\n"
                            moonExecCode += indent()+"$STORE_WORD ${child.moonVarName}(r0), r1\n"
                        }
                    }
                }
            }
            NodeLabel.IF.toString() -> {
                moonExecCode += indent("\n")+" % generating code for if branch\n"
                val relExpr = node.children[0]
                traverse(relExpr, symTab)

                val localRegister = registerPool.removeLast()
                val falseBlockLabel = getLabel()
                val skipFalseLabel = getLabel()

                // get the relexpr result and assess
                moonExecCode += indent("\n")+" % if condition\n"
                moonExecCode += indent()+"$LOAD_WORD $localRegister, ${relExpr.moonVarName}(r0)\n"
                moonExecCode += indent()+"$BRANCH_IF_ZERO $localRegister, $falseBlockLabel\n"

                val trueStatBlock = node.children[1]
                traverse(trueStatBlock, symTab)
                moonExecCode += indent()+"$JUMP $skipFalseLabel\n"
                moonExecCode += indent(falseBlockLabel)+"\n"
                val falseStatBlock = node.children[2]
                traverse(falseStatBlock, symTab)
                moonExecCode += indent(skipFalseLabel)+"\n"

                registerPool.add(localRegister)

            }
            NodeLabel.RELEXPR.toString() -> {
                moonExecCode += indent()+"% relexpr\n"
                val lhs = node.children[0]
                val op = node.children[1]
                val rhs = node.children[2]
                traverse(lhs, symTab)
                traverse(rhs, symTab)

                val tempVar = getTempVar()
                node.moonVarName = tempVar

                moonDataCode += indent()+"% space for temp variable\n"
                moonDataCode += indent(node.moonVarName)+"res 4\n"

                val localRegister = registerPool.removeLast()
                val leftRegister = registerPool.removeLast()
                val rightRegister = registerPool.removeLast()

                moonExecCode += indent()+"$LOAD_WORD $leftRegister, ${lhs.moonVarName}(r0)\n"
                moonExecCode += indent()+"$LOAD_WORD $rightRegister, ${rhs.moonVarName}(r0)\n"
                when (op.name) {
                    NodeLabel.EQ.toString() -> {
                        moonExecCode += indent()+"ceq $localRegister,$leftRegister,$rightRegister\n"
                    }
                    NodeLabel.NEQ.toString() -> {
                        moonExecCode += indent()+"cne $localRegister,$leftRegister,$rightRegister\n"
                    }
                    NodeLabel.LT.toString() -> {
                        moonExecCode += indent()+"clt $localRegister,$leftRegister,$rightRegister\n"
                    }
                    NodeLabel.GT.toString() -> {
                        moonExecCode += indent()+"cgt $localRegister,$leftRegister,$rightRegister\n"
                    }
                    NodeLabel.LEQ.toString() -> {
                        moonExecCode += indent()+"cle $localRegister,$leftRegister,$rightRegister\n"
                    }
                    NodeLabel.GEQ.toString() -> {
                        moonExecCode += indent()+"cge $localRegister,$leftRegister,$rightRegister\n"
                    }
                }
                moonExecCode += indent()+"$STORE_WORD ${node.moonVarName}(r0), $localRegister\n"

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
                moonExecCode += indent("\n")+" % generating code for while loop\n"
                val relExpr = node.children[0]

                val localRegister = registerPool.removeLast()
                val loopStartLabel = getLabel()
                val loopExitLabel = getLabel()

                moonExecCode += indent()+"% resetting registers\n"
                moonExecCode += indent()+"$SUB $localRegister,$localRegister,$localRegister\n"

                moonExecCode += loopStartLabel+"\n"
                traverse(relExpr, symTab)

                // get the relexpr result and assess
                moonExecCode += indent("\n")+" % while condition\n"
                moonExecCode += indent()+"$LOAD_WORD $localRegister, ${relExpr.moonVarName}(r0)\n"
                moonExecCode += indent()+"$BRANCH_IF_ZERO $localRegister, $loopExitLabel\n"

                val statBlock = node.children[1]
                traverse(statBlock, symTab)

                moonExecCode += indent()+"$JUMP $loopStartLabel\n"
                moonExecCode += loopExitLabel+"\n"

                registerPool.add(localRegister)
            }
            NodeLabel.FUNCCALL.toString() -> {
                // TODO
                // as functions are read, create and assign labels, find their labels in here and jump-link
                val funcId = node.children[0].t!!.lexeme
                val funcAParams = node.children[1].children


                val funcScope = global[funcId] ?: return
                if (funcScope !is semantic.Function)
                    return

                moonExecCode += indent("\n")+" % function call $funcId params\n"

                for (param in funcAParams)
                    traverse(param, symTab)
                moonExecCode += indent("\n")+" % function call $funcId\n"


                val localRegister = registerPool.removeLast()
                val offsetRegister = registerPool.removeLast()

                moonExecCode += indent()+"% resetting registers\n"
                moonExecCode += indent()+"$SUB $localRegister,$localRegister,$localRegister\n"
                moonExecCode += indent()+"$SUB $offsetRegister,$offsetRegister,$offsetRegister\n"

                var counter = 0
                for ((key,value) in funcScope.innerTable!!) {
                    if (value is Param) {
                        moonExecCode += indent()+"% matching function param\n"
                        val param = funcAParams[counter++]

                        if (param.moonOffsetLocation == "r0") {
                            moonExecCode += indent()+"$ADD_I $offsetRegister,r0,0\n"
                        }
                        else {
                            moonExecCode += indent() + "$LOAD_WORD $offsetRegister,${param.moonOffsetLocation}(r0)\n"
                        }
                        moonExecCode += indent()+"$ADD $offsetRegister,r0,$localRegister\n"
                        moonExecCode += indent()+"$LOAD_WORD $localRegister,${param.moonVarName}($offsetRegister)\n"
                        moonExecCode += indent()+"$STORE_WORD ${value.moonVarName}(r0),$localRegister\n"
                        println("")
                    }
                }
                /**
                 * if (lhs.moonOffsetLocation == "r0") {
                 *    moonExecCode += indent()+"$ADD_I $offsetRegister,r0,0\n"
                 * }
                 * else {
                 *    moonExecCode += indent()+"$LOAD_WORD $offsetRegister,${lhs.moonOffsetLocation}(r0)\n"
                 */
                moonExecCode += indent()+"$JUMP_LINK r14,${funcScope.moonLabel}"
                registerPool.add(offsetRegister)
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

    private fun getLabel(): String {
        return "L${labelCounter++}"
    }

    private fun indent(s: String = ""): String {
        return s.padEnd(lineUp)
    }

    private fun processDimList(dimlist: List<Node>): Int {
        var multDim = 1
        for (str in dimlist) {
            try {
                val parsedInt = str.t!!.lexeme.toInt()
                multDim *= parsedInt
            } catch (e: NumberFormatException) {
                // not a valid int
            }
        }
        return multDim
    }

}
