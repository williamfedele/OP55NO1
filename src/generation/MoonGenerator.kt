package generation

import ast.Node
import ast.NodeLabel
import semantic.*
import semantic.Function
import java.io.File
import java.io.FileWriter

class MoonGenerator (val global: HashMap<String, Entry>, val outputMoon: File) {

    init {
        FileWriter(outputMoon).use { out -> out.write("") }
    }
    private val lineUp = 8
    private var moonDataCode = ""
    private var moonExecCode = ""
    private val registerPool = ArrayDeque(REGISTERS)

    private var tempVarCounter = 0
    private var labelCounter = 0

    fun generate(node: Node?) {
        traverse(node, global)
        writeMoon(moonExecCode)
        writeMoon("\n$moonDataCode")
    }
    private fun traverse(node: Node?, symTab: HashMap<String, Entry>, context: Entry? = null) {
        if (node == null)
            return
        when (node.name) {
            /**
             * PROG node contains STRUCT, IMPL or FUNCDEF
             */
            NodeLabel.PROG.toString() -> {
                // carriage return used for printing
                moonDataCode += indent()+"% carriage return\n"
                moonDataCode += indent("cr")+"$DB 13,10\n"
                moonDataCode += indent()+"$ALIGN\n"

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
                val fn = symTab[funcId] as Function
                fn.moonLabel = getLabel()
                moonExecCode += fn.moonLabel

                // set return temp var
                // void return type doesn't get assigned a tempvar
                if (funcRet != "VOID") {
                    fn.moonReturnLabel = getTempVar()
                    when (funcRet) {
                        "INTEGER" -> {
                            moonDataCode += indent(fn.moonReturnLabel)+"$RES $INT_SIZE\n"
                        }
                        "FLOAT" -> {
                            println("float return variable not allocated")
                        }
                        "ID" -> {
                            println("id return variable not allocated")
                        }
                        else -> {println("unhandled return type in FUNCDEF: $funcRet")}
                    }
                }

                traverse(funcHead, fn.innerTable!!)

                // moon execution should start with the main function
                if (funcId == "main")
                    moonExecCode += indent("\n")+" $ENTRY\n"

                // handle all statements declared inside the function
                // provide the symbol table of the related function
                // if this is called from a free function, symTab = global
                // if called from an impl function, symTab = the classes symtab
                val innerTable = symTab[funcId]?.innerTable
                if (innerTable != null)
                    traverse(funcBody, innerTable, symTab[funcId])

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
                val paramMoonVar = paramName
                val paramSym = symTab[paramName] as Param
                paramSym.moonVarName = paramMoonVar

                // calculate the amount of allocations to make in the case of arrays
                val multDim = Util.processDimList(paramDimList)
                // if dimension is not provided, it must be an array passed by reference, no reserving
                if (multDim == 0) {
                    return
                }

                moonDataCode += indent("\n")+"% reserving space for a function parameter\n"
                // reserve space for the parameter temp variable
                when (paramType) {
                    "integer" -> {
                        moonDataCode += indent(paramMoonVar)+" res ${multDim * INT_SIZE}\n"
                    }
                    "float" -> {
                        moonDataCode += indent(paramMoonVar)+" res ${multDim * FLOAT_SIZE}\n"
                    }
                    else -> {
                        println("unhandle case in FPARAM: $paramName")
                    }
                }

            }
            /**
             * funcbody can have vardecl or statements
             */
            NodeLabel.FUNCBODY.toString() -> {
                for (child: Node in node.children) {
                    traverse(child, symTab, context)
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

                moonDataCode += indent() + "% space for variable $varId\n"

                // calculate the amount of allocations to make in the case of arrays

                val multDim = Util.processDimList(dimList)

                when (varType) {
                    "integer" -> {
                        moonDataCode += indent(varId)+" res ${multDim * INT_SIZE}\n"
                    }
                    "float" -> {
                        moonDataCode += indent(varId)+" res ${multDim * FLOAT_SIZE}\n"
                    }
                    else -> {
                        val classScope = global[varType] as Class
                        moonDataCode += indent(varId)+" res ${multDim * classScope.memSize}\n"
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

                val rightRegister = registerPool.removeLast()
                val offsetRegister = registerPool.removeLast()

                moonExecCode += indent()+"% resetting registers\n"
                moonExecCode += indent()+"$SUB $rightRegister,$rightRegister,$rightRegister\n"
                moonExecCode += indent()+"$SUB $offsetRegister,$offsetRegister,$offsetRegister\n"

                moonExecCode += indent("\n")+" % assignstat\n"

                // load rhs of comparison with offset applied
                if (rhs.moonOffsetLocation == ZERO_REG) {
                    moonExecCode += indent()+"$ADD_I $offsetRegister,$ZERO_REG,0\n"
                }
                else {
                    moonExecCode += indent() + "$LOAD_WORD $offsetRegister,${rhs.moonOffsetLocation}($ZERO_REG)\n"
                }
                // keep in register
                moonExecCode += indent()+"$LOAD_WORD $rightRegister,${rhs.moonVarName}($offsetRegister)\n"


                // load lhs of comparison with offset applied
                if (lhs.moonOffsetLocation == ZERO_REG) {
                    moonExecCode += indent()+"$ADD_I $offsetRegister,$ZERO_REG,0\n"
                }
                else {
                    moonExecCode += indent()+"$LOAD_WORD $offsetRegister,${lhs.moonOffsetLocation}($ZERO_REG)\n"
                }

                // assign lhs + offset (if array) = rhs
                moonExecCode += indent()+"$STORE_WORD ${lhs.moonVarName}($offsetRegister),$rightRegister\n"

                registerPool.add(offsetRegister)
                registerPool.add(rightRegister)
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
                moonDataCode += indent(node.moonVarName)+"$RES 4\n"
                moonExecCode += indent("\n")+" % putting int literal $intValue in tempvar ${node.moonVarName}\n"
                moonExecCode += indent()+"$ADD_I $localRegister,$ZERO_REG,$intValue\n"
                moonExecCode += indent()+"$STORE_WORD ${node.moonVarName}($ZERO_REG),$localRegister\n"

                registerPool.add(localRegister)
            }
            NodeLabel.FLOATLIT.toString() -> {
                node.moonVarName = getTempVar()
                val floatStr = node.t!!.lexeme
                var float = floatStr.toFloat()

                // if the parent node is a minus node, this is a negative float
                if (node.parent?.name == NodeLabel.MINUS.toString())
                    float *= -1

                // convert string to float and get bit representation
                val floatBits = float.toRawBits()

                // Moon only allows 16 bit literals when adding etc
                // get highest 16 bits by shifting right
                val floatHi = (floatBits shr 16).toShort()
                // get lowest 16 bis by doing a bitwise and with the highest 16-bit number
                val floatLo = (floatBits and 0b1111111111111111).toShort()

                val localRegister = registerPool.removeLast()

                moonDataCode += indent()+"% space for temp float literal\n"
                moonDataCode += indent(node.moonVarName)+"$RES $FLOAT_SIZE\n"

                // add float hi 16 bits
                moonExecCode += indent()+"$ADD_I $localRegister,$ZERO_REG,$floatHi\n"
                // shift left 16 bits
                moonExecCode += indent()+"$SHIFT_LEFT $localRegister,16\n"
                // add float lo 16 bits
                moonExecCode += indent()+"$ADD_I $localRegister,$localRegister,$floatLo\n"
                moonExecCode += indent()+"$STORE_WORD ${node.moonVarName}($ZERO_REG),$localRegister\n"

                registerPool.add(localRegister)
            }
            NodeLabel.ADDOP.toString() -> {
                node.moonVarName = getTempVar()

                traverse(node.children[0],symTab)
                val addOp = node.children[1]
                traverse(node.children[2], symTab)

                val localRegister = registerPool.removeLast()
                val leftRegister = registerPool.removeLast()
                val rightRegister = registerPool.removeLast()

                moonExecCode += indent("\n")+" % addop\n"
                moonExecCode += indent()+"$LOAD_WORD $leftRegister,${node.children[0].moonVarName}($ZERO_REG)\n"
                moonExecCode += indent()+"$LOAD_WORD $rightRegister,${node.children[2].moonVarName}($ZERO_REG)\n"

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

                moonDataCode += node.moonVarName.padEnd(lineUp)+"$DW 0\n"
                moonExecCode += indent()+"$STORE_WORD ${node.moonVarName}($ZERO_REG),$localRegister\n"

                registerPool.add(rightRegister)
                registerPool.add(leftRegister)
                registerPool.add(localRegister)
            }
            NodeLabel.MULTOP.toString() -> {
                node.moonVarName = getTempVar()
                traverse(node.children[0], symTab)
                val multOp = node.children[1]
                traverse(node.children[2], symTab)

                val localRegister = registerPool.removeLast()
                val leftRegister = registerPool.removeLast()
                val rightRegister = registerPool.removeLast()

                moonExecCode += indent("\n")+" % multop\n"
                moonExecCode += indent()+"$LOAD_WORD $leftRegister,${node.children[0].moonVarName}($ZERO_REG)\n"
                moonExecCode += indent()+"$LOAD_WORD $rightRegister,${node.children[2].moonVarName}($ZERO_REG)\n"

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

                moonDataCode += node.moonVarName.padEnd(lineUp)+"$DW 0\n"
                moonExecCode += indent()+"$STORE_WORD ${node.moonVarName}($ZERO_REG),$localRegister\n"

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
                if (node.children[0].name == "DOT") {
                    traverse(node.children[0],symTab)
                    node.moonVarName = node.children[0].moonVarName
                    node.moonOffsetLocation = node.children[0].moonOffsetLocation
                    return
                }

                val varId = node.children[0].t!!.lexeme
                val varIndiceList = node.children[1].children
                val symTabEntry = symTab[varId]

                if (varIndiceList.isNotEmpty() && (symTabEntry is Local || symTabEntry is Param)) {
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

                    val localEntry = symTab[varId]
                    var arrShape = listOf<String>()
                    var varType = ""

                    if (localEntry is Local) {
                        arrShape = localEntry.variable.dim
                        varType = localEntry.variable.type
                    }
                    else if (localEntry is Param) {
                        arrShape = localEntry.variable.dim
                        varType = localEntry.variable.type
                    }

                    // n-dimensional arrays are stored in contiguous memory
                    // calculate the stride to ensure we skip over dimensions as we index the array
                    // Ex: for a [2][4][3] array:
                    //      first dimension changes require stepping over 12 (4*3) integers (48 bytes)
                    //      second dimension changes require stepping over 3 integers (12 bytes)
                    //      third dimension changes require stepping over 1 integer (4 bytes)
                    val strides = MutableList(arrShape.size){1}
                    for (i in arrShape.indices) {
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
                            // an int value index is simply added to the accumulatorRegister
                            NodeLabel.INTLIT.toString() -> {
                                val intValue = varIndiceList[i].t!!.lexeme.toInt()
                                if (varType == "integer" || varType == "float") {
                                    val offset = intValue * INT_SIZE * strides[i]
                                    moonExecCode += indent()+"$ADD_I $accumulatorRegister,$accumulatorRegister,$offset\n"
                                }

                            }
                            // variable indexes must first be loaded from memory then added to the accumulatorRegister
                            else -> {
                                // add offset of variable
                                // if offset is a temp variable, load it
                                moonExecCode += indent()+"% variable indice detected\n"
                                if (varIndiceList[i].moonOffsetLocation == ZERO_REG) {
                                    moonExecCode += indent()+"$ADD $offsetRegister,$ZERO_REG,${varIndiceList[i].moonOffsetLocation}\n"
                                } else {
                                    moonExecCode += indent()+"$LOAD_WORD $offsetRegister,${varIndiceList[i].moonOffsetLocation}($ZERO_REG)\n"
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
                    moonDataCode += indent(node.moonOffsetLocation)+"$RES 4\n"
                    moonExecCode += indent()+"$STORE_WORD ${node.moonOffsetLocation}($ZERO_REG),$accumulatorRegister\n"

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
                if (child.moonOffsetLocation == ZERO_REG) {
                    moonExecCode += indent()+"$ADD_I $localRegister,$ZERO_REG,0\n"
                }
                else {
                    moonExecCode += indent()+"$LOAD_WORD $localRegister,${child.moonOffsetLocation}($ZERO_REG)\n"
                }
                moonExecCode += indent()+"$ADD $offsetRegister,$ZERO_REG,$localRegister\n"
                moonExecCode += indent()+"$LOAD_WORD $UTIL_IO_REG,${child.moonVarName}($offsetRegister)\n"
                moonExecCode += indent()+"$JUMP_LINK $UTIL_JUMP_REG, putint\n"

                // output a newline
                moonExecCode += indent()+"$ADD_I $UTIL_IO_REG,$ZERO_REG,cr\n"
                moonExecCode += indent()+"$JUMP_LINK $UTIL_JUMP_REG,putstr\n"
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
                            moonExecCode += indent()+"$JUMP_LINK $UTIL_JUMP_REG,getint\n"
                            moonExecCode += indent()+"$STORE_WORD ${child.moonVarName}($ZERO_REG),$UTIL_IO_REG\n"
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
                moonExecCode += indent()+"$LOAD_WORD $localRegister,${relExpr.moonVarName}($ZERO_REG)\n"
                moonExecCode += indent()+"$BRANCH_IF_ZERO $localRegister,$falseBlockLabel\n"

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
                moonDataCode += indent(node.moonVarName)+"$RES 4\n"

                val localRegister = registerPool.removeLast()
                val offsetRegister = registerPool.removeLast()
                val leftRegister = registerPool.removeLast()
                val rightRegister = registerPool.removeLast()

                moonExecCode += indent()+"% resetting registers\n"
                moonExecCode += indent()+"$SUB $offsetRegister,$offsetRegister,$offsetRegister\n"

                // load lhs of comparison with offset applied
                if (lhs.moonOffsetLocation == ZERO_REG) {
                    moonExecCode += indent()+"$ADD_I $offsetRegister,$ZERO_REG,0\n"
                }
                else {
                    moonExecCode += indent() + "$LOAD_WORD $offsetRegister,${lhs.moonOffsetLocation}($ZERO_REG)\n"
                }
                moonExecCode += indent()+"$LOAD_WORD $leftRegister,${lhs.moonVarName}($offsetRegister)\n"

                // load rhs of comparison with offset applied
                if (rhs.moonOffsetLocation == ZERO_REG) {
                    moonExecCode += indent()+"$ADD_I $offsetRegister,$ZERO_REG,0\n"
                }
                else {
                    moonExecCode += indent() + "$LOAD_WORD $offsetRegister,${rhs.moonOffsetLocation}($ZERO_REG)\n"
                }
                moonExecCode += indent()+"$LOAD_WORD $rightRegister,${rhs.moonVarName}($offsetRegister)\n"
                when (op.name) {
                    NodeLabel.EQ.toString() -> {
                        moonExecCode += indent()+"$EQUAL $localRegister,$leftRegister,$rightRegister\n"
                    }
                    NodeLabel.NEQ.toString() -> {
                        moonExecCode += indent()+"$NOT_EQUAL $localRegister,$leftRegister,$rightRegister\n"
                    }
                    NodeLabel.LT.toString() -> {
                        moonExecCode += indent()+"$LESS $localRegister,$leftRegister,$rightRegister\n"
                    }
                    NodeLabel.GT.toString() -> {
                        moonExecCode += indent()+"$GREATER $localRegister,$leftRegister,$rightRegister\n"
                    }
                    NodeLabel.LEQ.toString() -> {
                        moonExecCode += indent()+"$LESS_EQUAL $localRegister,$leftRegister,$rightRegister\n"
                    }
                    NodeLabel.GEQ.toString() -> {
                        moonExecCode += indent()+"$GREATER_EQUAL $localRegister,$leftRegister,$rightRegister\n"
                    }
                }
                moonExecCode += indent()+"$STORE_WORD ${node.moonVarName}($ZERO_REG),$localRegister\n"

                registerPool.add(rightRegister)
                registerPool.add(leftRegister)
                registerPool.add(offsetRegister)
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
                moonExecCode += indent()+"$LOAD_WORD $localRegister,${relExpr.moonVarName}($ZERO_REG)\n"
                moonExecCode += indent()+"$BRANCH_IF_ZERO $localRegister,$loopExitLabel\n"

                val statBlock = node.children[1]
                traverse(statBlock, symTab)

                moonExecCode += indent()+"$JUMP $loopStartLabel\n"
                moonExecCode += loopExitLabel+"\n"

                registerPool.add(localRegister)
            }
            NodeLabel.FUNCCALL.toString() -> {
                val funcId = node.children[0].t!!.lexeme
                val funcAParams = node.children[1].children

                val funcScope = global[funcId] ?: return
                if (funcScope !is Function)
                    return

                // save return label in node for usage in assignstats
                node.moonVarName = funcScope.moonReturnLabel

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

                        moonExecCode += indent()+"% resetting registers\n"
                        moonExecCode += indent()+"$SUB $localRegister,$localRegister,$localRegister\n"
                        moonExecCode += indent()+"$SUB $offsetRegister,$offsetRegister,$offsetRegister\n"
                        moonExecCode += indent()+"% matching function param\n"
                        val param = funcAParams[counter++]

                        val entry = symTab[key]
                        var dim = 1
                        if (entry is Local)
                            dim = getArraySize(entry.variable.dim)

                        if (dim == 1) {
                            moonExecCode += indent()+"% resetting registers\n"
                            moonExecCode += indent()+"$SUB $localRegister,$localRegister,$localRegister\n"
                            if (param.moonOffsetLocation == ZERO_REG) {
                                moonExecCode += indent()+"$ADD_I $offsetRegister,$ZERO_REG,0\n"
                            }
                            else {
                                moonExecCode += indent() + "$LOAD_WORD $offsetRegister,${param.moonOffsetLocation}($ZERO_REG)\n"
                            }
                            moonExecCode += indent()+"$LOAD_WORD $localRegister,${param.moonVarName}($offsetRegister)\n"
                            moonExecCode += indent()+"$STORE_WORD ${value.moonVarName}($ZERO_REG),$localRegister\n"
                        }
                        else {
                            // copy each 4 byte word from the array over
                            for (i in 0..<dim) {
                                // load current offset
                                moonExecCode += indent()+"$ADD_I $offsetRegister,$ZERO_REG,${i*4}\n"
                                // load the array value at the offset
                                moonExecCode += indent()+"$LOAD_WORD $localRegister,${param.moonVarName}($offsetRegister)\n"
                                // save the value in the parameter
                                moonExecCode += indent()+"$STORE_WORD ${value.moonVarName}($offsetRegister),$localRegister\n"
                            }
                        }


                    }
                }
                moonExecCode += indent()+"$JUMP_LINK r14,${funcScope.moonLabel}\n"
                registerPool.add(offsetRegister)
                registerPool.add(localRegister)
            }
            NodeLabel.RETURN.toString() -> {
                if (context is Function) {
                    val returnVar = node.children[0]
                    traverse(returnVar, symTab)

                    val localRegister = registerPool.removeLast()
                    val offsetRegister = registerPool.removeLast()

                    moonExecCode += indent()+"% resetting registers\n"
                    moonExecCode += indent()+"$SUB $localRegister,$localRegister,$localRegister\n"
                    if (returnVar.moonOffsetLocation == ZERO_REG) {
                        moonExecCode += indent()+"$ADD_I $offsetRegister,$ZERO_REG,0\n"
                    }
                    else {
                        moonExecCode += indent() + "$LOAD_WORD $offsetRegister,${returnVar.moonOffsetLocation}($ZERO_REG)\n"
                    }
                    moonExecCode += indent()+"$LOAD_WORD $localRegister,${returnVar.moonVarName}($offsetRegister)\n"
                    moonExecCode += indent()+"$STORE_WORD ${context.moonReturnLabel}($ZERO_REG),$localRegister\n"
                    moonExecCode += indent()+"$JUMP_REGISTER r14\n"

                    registerPool.add(offsetRegister)
                    registerPool.add(localRegister)
                }

            }
            NodeLabel.PLUS.toString() -> {
                traverse(node.children[0], symTab)
                node.moonVarName = node.children[0].moonVarName
                node.moonOffsetLocation = node.children[0].moonOffsetLocation

            }
            NodeLabel.MINUS.toString() -> {
                traverse(node.children[0], symTab)
                node.moonVarName = node.children[0].moonVarName
                node.moonOffsetLocation = node.children[0].moonOffsetLocation

            }
            NodeLabel.STRUCT.toString() -> {
                // TODO
            }
            NodeLabel.IMPLDEF.toString() -> {
                // TODO
            }
            NodeLabel.DOT.toString() -> {
                for (child in node.children)
                    traverse(child, symTab)
                val lhs = node.children[0]
                val rhs = node.children[1]
                var rhsName = ""
                if (rhs.name == "ID"){
                    rhsName = rhs.t!!.lexeme
                }
                else
                    rhsName = rhs.moonVarName

                node.moonVarName = lhs.moonVarName

                val varScope = symTab[lhs.moonVarName]
                if (varScope is Local) {
                    val varType = varScope.variable.type
                    val classScope = global[varType]
                    if (classScope is Class) {
                        val dataMemberScope = classScope.innerTable?.get(rhsName)
                        if (dataMemberScope is Data) {
                            node.moonOffsetLocation = getTempVar()
                            val offsetRegister = registerPool.removeLast()
                            moonDataCode += indent()+"% space for temp indice\n"
                            moonDataCode += indent(node.moonOffsetLocation)+"$RES 4\n"
                            moonExecCode += indent()+"$ADD_I $offsetRegister,$ZERO_REG,${dataMemberScope.memOffset}\n"
                            moonExecCode += indent()+"$STORE_WORD ${node.moonOffsetLocation}($ZERO_REG),$offsetRegister\n"
                            registerPool.add(offsetRegister)
                        }
                    }
                }
            }
            else -> {}
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

    private fun getArraySize(dimList: MutableList<String>): Int {

        var dim = 1
        for (s in dimList) {
            val parsedInt = s.toInt()
            dim *= parsedInt
        }
        return dim
    }

}
