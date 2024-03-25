package semantic

import ast.Node
import ast.NodeLabel
import lexer.TokenType
import java.io.File
import java.io.FileWriter

class SymbolTableCreator(val outputSymbolTables: File, val outputSemanticErrors: File) {
    init {
        // Create/Clear the output files
        FileWriter(outputSymbolTables).use { out -> out.write("") }
        FileWriter(outputSemanticErrors).use { out -> out.write("") }
    }

    var global: HashMap<String, Entry> = linkedMapOf()

    fun create(node: Node?, scope: HashMap<String, Entry> = global, entry: Entry? = null) {
        if (node == null)
            return
        when (node.name) {
            NodeLabel.PROG.toString() -> {
                for (child: Node in node.children) {
                    create(child)
                }
            }
            NodeLabel.STRUCT.toString() -> {
                val structIdToken = node.children[0].t!!
                val structId = structIdToken.lexeme
                val inheritList = node.children[1]
                val structdecls = node.children[2]

                var innerScope: HashMap<String, Entry> = linkedMapOf()

                if (global.containsKey(structId) && global[structId]!!.innerTable != null) {
                    writeError("Multiply declared class: $structId on line ${structIdToken.line}.")
                    innerScope = global[structId]!!.innerTable!!
                }
                else {
                    // inherit scope member
                    val inherits = mutableListOf<String>()
                    for (n: Node in inheritList.children) {
                        inherits.add(n.t!!.lexeme)
                    }
                    global[structId] = Class(
                        structId,
                        innerScope,
                        inherits
                    )
                }
                for (child: Node in structdecls.children) {
                    create(child, innerScope, global[structId])
                }
            }
            NodeLabel.STRUCTFUNCHEAD.toString() -> {
                val visibility = node.children[0].t!!.lexeme
                val funcHeadToken = node.children[1].t!!
                val funcId = funcHeadToken.lexeme
                val funcParams = node.children[2]
                val returnType = node.children[3].t!!.lexeme

                if (isDuplicateDefinition(funcId, funcParams.children, scope)) {
                    writeError("Multiply defined member function: $funcId on line ${funcHeadToken.line}.")
                    return
                }
                else if (scope.containsKey(funcId)) {
                    writeWarning("Overloaded member function: $funcId on line ${funcHeadToken.line}.")
                    return
                }

                val classEntry = entry as Class

                if (classEntry.inherits.isNotEmpty()) {
                    for (inher in classEntry.inherits) {
                        val inherScope = global[inher]
                        if (inherScope != null && inherScope.innerTable?.get(funcId) != null) {
                            writeWarning("Overridden inherited member function: $funcId on line ${funcHeadToken.line}.")
                        }
                    }
                }

                val innerScope: HashMap<String, Entry> = linkedMapOf()

                val paramList = mutableListOf<Variable>()
                for (param: Node in funcParams.children) {
                    val paramId = param.children[0].t!!.lexeme
                    val paramType = param.children[1].t!!.lexeme
                    val paramDimList = param.children[2]

                    val typeDim = Variable(paramType)
                    for (dim: Node in paramDimList.children) {
                        typeDim.dim.add(dim.t!!.lexeme)
                    }

                    innerScope[paramId] = Param(
                        paramId,
                        typeDim
                    )
                    paramList.add(typeDim)
                }

                scope[funcId] = Function(
                    funcId,
                    innerScope,
                    entry?.name,
                    visibility,
                    returnType,
                    paramList
                )
            }
            NodeLabel.STRUCTVARDECL.toString() -> {
                val visibility = node.children[0].t!!.lexeme
                val varIdToken = node.children[1].t!!
                val varId = varIdToken.lexeme
                val dataType = node.children[2].t!!.lexeme
                val dataTypeDimList = node.children[3]

                // check for duplicate variable id
                if (scope.containsKey(varId)) {
                    writeError("Multiply declared identifier in class: $varId on line ${varIdToken.line}.")
                    return
                }

                val variable = Variable(dataType)
                // collect params
                for (dim: Node in dataTypeDimList.children) {
                    variable.dim.add(dim.t!!.lexeme)
                }

                scope[varId] = Data(
                    varId,
                    variable,
                    visibility
                )
            }
            NodeLabel.IMPLDEF.toString() -> {
                val implToken = node.children[0].t!!
                val implId = implToken.lexeme
                val implDefs = node.children[1]

                // check impl name exists
                val implScope = global[implId]
                if (implScope == null) {
                    writeError("Undeclared class definition: $implId on line ${implToken.line}.")
                    return
                }
                val funcList = mutableListOf<String>()
                if (implScope.innerTable != null) {
                    for ((key, value) in implScope.innerTable.entries) {
                        if (value is Function) {
                            funcList.add(key)
                        }
                    }
                }

                for (funcDef: Node in implDefs.children) {
                    val funcHead = funcDef.children[0]
                    val funcHeadToken = funcHead.children[0].t!!
                    val funcId = funcHeadToken.lexeme
                    val funcParams = funcHead.children[1]
                    val funcRet = funcHead.children[2].t!!.lexeme

                    // check func exists within the impl
                    // val funcScope = implScope.innerTable?.get(funcId)
                    if (implScope.innerTable?.get(funcId) == null) {
                        writeError("Undeclared member function definition: $funcId on line ${funcHeadToken.line}.")
                    }
                    else {
                        funcList.remove(funcId)

                        val funcBody = funcDef.children[1]
                        val funcScope = implScope.innerTable[funcId]
                        if (funcScope?.innerTable != null)
                            create(funcBody, funcScope.innerTable, funcScope)
                    }
                }
                for (funcId in funcList) {
                    writeError("Undefined member function declaration: $funcId in class $implId.")
                }
            }
            NodeLabel.FUNCDEF.toString() -> {
                // only used for free functions.
                // impl functions fetch the associated function defined from a struct

                val funcHead = node.children[0]
                val funcHeadToken = funcHead.children[0].t!!
                val funcId = funcHeadToken.lexeme
                create(funcHead, scope)

                val funcBody = node.children[1]
                if (scope[funcId] != null && scope[funcId]?.innerTable != null)
                    create(funcBody, scope[funcId]?.innerTable!!, scope[funcId])
            }
            NodeLabel.FUNCHEAD.toString() -> {
                val funcHeadToken = node.children[0].t!!
                val funcId = funcHeadToken.lexeme
                val funcParams = node.children[1]
                val funcReturn = node.children[2].t!!.lexeme

                // check for duplicate free function definition
                if (isDuplicateDefinition(funcId, funcParams.children, scope)) {
                    writeError("Multiply defined free function: $funcId on line ${funcHeadToken.line}.")
                    return
                }
                // if not duplicate but same function name = overloaded
                else if (scope.containsKey(funcId)) {
                    writeWarning("Overloaded free function: $funcId on line ${funcHeadToken.line}")
                }

                val innerScope: HashMap<String, Entry> = linkedMapOf()

                val paramList = mutableListOf<Variable>()
                for (param: Node in funcParams.children) {
                    val paramId = param.children[0].t!!.lexeme
                    val paramType = param.children[1].t!!.lexeme
                    val paramDimList = param.children[2]

                    val typeAndDim = Variable(paramType)
                    for (dim: Node in paramDimList.children) {
                        val dimValue = if (dim.name == NodeLabel.EMPTY.toString())
                            ""
                        else
                            dim.t!!.lexeme

                        typeAndDim.dim.add(dimValue)
                    }
                    innerScope[paramId] = Param(
                        paramId,
                        typeAndDim
                    )
                    paramList.add(typeAndDim)
                }

                scope[funcId] = Function(
                    funcId,
                    innerScope,
                    null,
                    "public",
                    funcReturn,
                    paramList
                )
            }
            NodeLabel.FUNCBODY.toString() -> {
                for (child: Node in node.children) {
                    when (child.name) {
                        NodeLabel.IF.toString() -> {
                            // handle if statements
                        }
                        NodeLabel.WHILE.toString() -> {
                            // handle while statements
                        }
                        NodeLabel.FUNCCALL.toString() -> {
                            // handle func calls
                        }
                        NodeLabel.ASSIGNSTAT.toString() -> {
                            //handle assign statements
                        }
                        NodeLabel.RETURN.toString() -> {
                            //handle return statements
                            val returnToken = child.children[0].t
                            val funcEntry = entry as Function
                            if (returnToken == null)
                                return

                            if (funcEntry.returnType == "integer" && returnToken.type != TokenType.INTNUM)
                                writeError("Type error in return statement: ${returnToken.lexeme} on line ${returnToken.line}, integer expected.")
                            else if (funcEntry.returnType == "float" && returnToken.type != TokenType.FLOATNUM)
                                writeError("Type error in return statement: ${returnToken.lexeme} on line ${returnToken.line}, float expected.")
                            else if (funcEntry.returnType != "integer" && funcEntry.returnType != "float" && returnToken.type != TokenType.ID)
                                writeError("Type error in return statement: ${returnToken.lexeme} on line ${returnToken.line}, id expected")
                            //val i = 0
                        }
                        NodeLabel.VARDECL.toString() -> {
                            //handle var declarations
                            val varIdToken = child.children[0].t!!
                            val varId = varIdToken.lexeme
                            val varType = child.children[1].t!!.lexeme
                            val varTypeDimList = child.children[2]

                            // check for duplicate local variable IDs
                            if (scope.containsKey(varId)) {
                                writeError("Multiply declared identifier in function: $varId on line ${varIdToken.line}.")
                                return
                            }

                            // check for data member shadowing
                            val funcEntry = entry as Function
                            if (funcEntry.parentClass != null) {
                                val inheritedClass = global[funcEntry.parentClass]
                                if (inheritedClass != null) {
                                    val inheritedScope = inheritedClass.innerTable
                                    if (inheritedScope?.containsKey(varId) == true) {
                                        writeWarning("Shadowed inherited data member: $varId on line ${varIdToken.line}")
                                    }
                                }
                            }

                            val dimList = mutableListOf<String>()
                            for (dim: Node in varTypeDimList.children) {
                                dimList.add(dim.t!!.lexeme)
                            }

                            val typeAndDim = Variable(varType, dimList)

                            val varDecl = Local(
                                varId,
                                typeAndDim
                            )
                            scope[varId] = varDecl
                        }
                        else -> {println("Missing a case for ${child.name} in the FUNCBODY node handler.")}
                    }
                }
            }
        }
    }

    /**
     * Entry point for a DFS style traversal of the hashmap symbol table
     * Since a non-empty top level table implies it's a global table, this is explicitly printed.
     *   There is no table naming at this level.
     */
    fun dfs() {
        if (global.isNotEmpty()) {
            writeOut("|    table: global")
            writeOut("|    =============================================================")
            dfs2()
        }
    }
    private fun dfs2(scope: HashMap<String, Entry> = global, padding: String = "|") {
        for ((key, value) in scope) {
            writeOut("$padding    $value")
            // separator for classes
            if (value is Class)
                writeOut("$padding    =============================================================")

            if (value.innerTable != null) {
                writeOut("$padding        table: $key")
                writeOut("$padding        =============================================================")
                dfs2(value.innerTable, "$padding    ")
            }
        }
    }

    /**
     * Detects duplicate function definitions.
     * Duplicate definitions are those that have the same identifier and the same parameter list.
     * If the parameter list is different, the function is overloaded instead.
     */
    private fun isDuplicateDefinition(funcId: String, funcParams: List<Node>, scope: HashMap<String, Entry>): Boolean {
        // function name must be the same to be considered potential duplicate
        if (!scope.containsKey(funcId))
            return false

        // the function name must be a function. A function is allowed to have the same name as a class for example.
        if (scope[funcId] !is Function)
            return false

        val funcScope = scope[funcId] as Function

        // build param list in the same way as it's done for function definitions
        val paramList = mutableListOf<Variable>()
        for (param: Node in funcParams) {
            val paramType = param.children[1].t!!.lexeme
            val paramDimList = param.children[2]

            val typeDim = Variable(paramType)
            for (dim: Node in paramDimList.children) {
                typeDim.dim.add(dim.t!!.lexeme)
            }
            paramList.add(typeDim)
        }
        // if the parameters are the same, this is a duplicate definition.
        return paramList == funcScope.params
    }

    private fun writeOut(s: String) {
        FileWriter(outputSymbolTables, true).use { out -> out.write("$s\n") }
    }

    private fun writeError(s: String) {
        FileWriter(outputSemanticErrors, true).use { out -> out.write("ERROR   - $s\n") }
    }

    private fun writeWarning(s: String) {
        FileWriter(outputSemanticErrors, true).use { out -> out.write("WARNING - $s\n") }
    }
}

data class Variable (val type: String, val dim: MutableList<String> = mutableListOf()) {
    override fun toString(): String {
        if (dim.isNotEmpty()) {
            var ret = "$type["
            ret += dim.joinToString("][")
            ret += "]"
            return ret
        }
        else {
            return type
        }
    }

    override fun equals(other: Any?): Boolean {
        return (other is Variable) && type == other.type && dim == other.dim
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + dim.hashCode()
        return result
    }
}
