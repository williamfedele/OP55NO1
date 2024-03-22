package semantic

import ast.Node
import ast.NodeLabel
import java.io.File
import java.io.FileWriter
import java.util.*

class SymbolTableCreator(val outputSymbolTables: File, val outputSemanticErrors: File) {
    init {
        FileWriter(outputSymbolTables).use { out -> out.write("") }
        FileWriter(outputSemanticErrors).use { out -> out.write("") }
    }

    var global: HashMap<String, Base> = linkedMapOf()

    fun create(node: Node?, scope: HashMap<String, Base> = global) {
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

                var innerScope: HashMap<String, Base> = linkedMapOf()

                if (global.containsKey(structId)) {
                    writeError("Multiply declared class: $structId on line ${structIdToken.line}.")
                    innerScope = global[structId]!!.innerTable
                }
                else {
                    // inherit scope member
                    val inherits = mutableListOf<String>()
                    for (n: Node in inheritList.children) {
                        inherits.add(n.t!!.lexeme)
                    }
                    global[structId] = Class(
                        structId,
                        Kind.CLASS,
                        innerScope,
                        inherits
                    )
                }
                for (child: Node in structdecls.children) {
                    create(child, innerScope)
                }
            }
            NodeLabel.STRUCTFUNCHEAD.toString() -> {
                val visibility = node.children[0].t!!.lexeme
                val funcId = node.children[1].t!!.lexeme
                val funcParams = node.children[2]
                val returnType = node.children[3].t!!.lexeme

                val innerScope: HashMap<String, Base> = linkedMapOf()

                val paramList = mutableListOf<Variable>()
                for (param: Node in funcParams.children) {
                    val paramId = param.children[0].t!!.lexeme
                    val paramType = param.children[1].t!!.lexeme
                    val paramDimList = param.children[2]

                    val typeDim = Variable(paramType)
                    for (dim: Node in paramDimList.children) {
                        typeDim.dim.add(dim.t!!.lexeme)
                    }
                    val paramScope: HashMap<String, Base> = linkedMapOf()
                    innerScope[paramId] = Param(
                        paramId,
                        Kind.PARAM,
                        paramScope,
                        typeDim
                    )
                    paramList.add(typeDim)
                }


                scope[funcId] = Function(
                    funcId,
                    Kind.FUNCTION,
                    innerScope,
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

                if (scope.containsKey(varId)) {
                    writeError("Multiply declared identifier in class: $varId on line ${varIdToken.line}.")
                }
                val variable = Variable(dataType)
                // collect params
                for (dim: Node in dataTypeDimList.children) {
                    variable.dim.add(dim.t!!.lexeme)
                }

                val innerScope: HashMap<String, Base> = linkedMapOf()
                scope[varId] = StructVarDecl(
                    varId,
                    Kind.DATA,
                    innerScope,
                    variable,
                    visibility
                )
            }
            NodeLabel.IMPLDEF.toString() -> {
                val implToken = node.children[0].t!!
                val implId = implToken.lexeme
                val implDefs = node.children[1]

                for (funcDef: Node in implDefs.children) {
                    val funcHead = funcDef.children[0] // TODO, validate impl header matches struct declaration for errors
                    val funcHeadToken = funcHead.children[0].t!!
                    val funcId = funcHeadToken.lexeme
                    val funcParams = funcHead.children[1]
                    val funcRet = funcHead.children[2].t!!.lexeme

                    val paramList = mutableListOf<Variable>()
                    for (param: Node in funcParams.children) {
                        val paramType = param.children[1].t!!.lexeme
                        val paramDimList = param.children[2]

                        val typeDim = Variable(paramType)
                        for (dim: Node in paramDimList.children) {
                            typeDim.dim.add(dim.t!!.lexeme)
                        }
                        paramList.add(typeDim)
                    }

                    val implScope = global[implId]
                    if (implScope == null) {
                        writeError("Undeclared class definition: $implId on line ${implToken.line}.")
                    }
                    else {
                        val funcScope = implScope.innerTable[funcId]
                        if (funcScope == null) {
                            writeError("Undeclared member function definition: $funcId on line ${funcHeadToken.line}.")
                        }
                        else {
                            val funcBody = funcDef.children[1]
                            create(funcBody, funcScope.innerTable)
                        }
                    }
                }
            }
            NodeLabel.FUNCDEF.toString() -> {
                // only used for free functions.
                // impl functions fetch the associated function defined from a struct

                val funcHead = node.children[0]
                val funcHeadToken = funcHead.children[0].t!!
                val funcId = funcHeadToken.lexeme

                if (scope.containsKey(funcId) && scope[funcId]!!.type == Kind.FUNCTION) {
                    writeError("Multiply defined free function: $funcId on line ${funcHeadToken.line}")
                }

                create(funcHead, scope)

                val funcBody = node.children[1]
                create(funcBody, scope[funcId]!!.innerTable)
            }
            NodeLabel.FUNCHEAD.toString() -> {
                val funcId = node.children[0].t!!.lexeme
                val funcParams = node.children[1]
                val funcReturn = node.children[2].t!!.lexeme

                val innerScope: HashMap<String, Base> = linkedMapOf()

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
                    val paramScope: HashMap<String, Base> = linkedMapOf()
                    innerScope[paramId] = Param(
                        paramId,
                        Kind.PARAM,
                        paramScope,
                        typeAndDim
                    )
                    paramList.add(typeAndDim)
                }


                scope[funcId] = Function(
                    funcId,
                    Kind.FUNCTION,
                    innerScope,
                    "public",
                    funcReturn,
                    paramList
                )
            }
            NodeLabel.FUNCBODY.toString() -> {
                for (child: Node in node.children) {
                    when (child.name) {
                        NodeLabel.RETURN.toString() -> {
                            //handle return statements
                        }
                        NodeLabel.ASSIGNSTAT.toString() -> {
                            //handle assign statements
                        }
                        NodeLabel.VARDECL.toString() -> {
                            //handle var declarations
                            val varIdToken = child.children[0].t!!
                            val varId = varIdToken.lexeme
                            val varType = child.children[1].t!!.lexeme
                            val varTypeDimList = child.children[2]

                            // local defined variables cannot have duplicate IDs
                            if (scope.containsKey(varId)) {
                                writeError("Multiply declared identifier in function: $varId on line ${varIdToken.line}.")
                                return
                            }

                            val innerScope: HashMap<String, Base> = linkedMapOf()

                            val dimList = mutableListOf<String>()
                            for (dim: Node in varTypeDimList.children) {
                                dimList.add(dim.t!!.lexeme)
                            }

                            val typeAndDim = Variable(varType, dimList)

                            val varDecl = FuncVarDecl(
                                varId,
                                Kind.LOCAL,
                                innerScope,
                                typeAndDim
                            )
                            scope[varId] = varDecl
                        }
                        else -> {println("Missing a case for ${child.name} in the FUNCBODY symtab handler.")}
                    }
                }
            }
        }
    }

    fun dfs() {
        // the top level hashmap is the global scope
        // it doesn't have any sort of label so its printed here
        if (global.isNotEmpty()) {
            writeOut("|    table: global")
            writeOut("|    =============================================================")
            dfs2()
        }
    }
    private fun dfs2(scope: HashMap<String, Base> = global, padding: String = "|") {
        if (scope.isEmpty())
            return

        for ((key, value) in scope) {
            writeOut("$padding    $value")
            if (value.type == Kind.CLASS)
                writeOut("$padding    =============================================================")

            if (value.innerTable.isNotEmpty()) {
                writeOut("$padding        table: $key")
                writeOut("$padding        =============================================================")
            }
            dfs2(value.innerTable, "$padding    ")
        }
    }

    private fun writeOut(s: String) {
        FileWriter(outputSymbolTables, true).use { out -> out.write("$s\n") }
    }

    private fun writeError(s: String) {
        FileWriter(outputSemanticErrors, true).use { out -> out.write("ERROR - $s\n") }
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
}

enum class Kind {
    CLASS,
    FUNCTION,
    DATA,
    PARAM,
    LOCAL;

    override fun toString(): String {
        return super.toString().lowercase(Locale.getDefault())
    }
}

