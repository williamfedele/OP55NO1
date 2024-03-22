package semantic

import ast.Node
import ast.NodeLabel
import java.util.*

class SymbolTableCreator {
    var table: HashMap<String, Base> = linkedMapOf()

    fun create(node: Node?, scope: HashMap<String, Base> = table) {
        if (node == null)
            return
        when (node.name) {
            NodeLabel.PROG.toString() -> {
                // somehow create global scope? maybe just the hashmap creation
                for (child: Node in node.children) {
                    create(child)
                }
            }
            NodeLabel.STRUCT.toString() -> {
                val structIdToken = node.children[0].t!!
                val structId = structIdToken.lexeme
                val inherits = node.children[1]
                val structdecls = node.children[2]

                val inheritsList = mutableListOf<String>()
                for (n: Node in inherits.children) {
                    inheritsList.add(n.t!!.lexeme)
                }
                val innerScope = HashMap<String, Base>()
                table[structId] = Class(
                    structId,
                    EntryType2.CLASS,
                    innerScope,
                    inheritsList)

                for (child: Node in structdecls.children) {
                    create(child, innerScope)
                }
            }
            NodeLabel.STRUCTFUNCHEAD.toString() -> {
                //val parentSymTab = node.symTab
                val visibility = node.children[0].t!!.lexeme
                val funcId = node.children[1].t!!.lexeme
                val funcParams = node.children[2]
                val returnType = node.children[3].t!!.lexeme

                val innerScope: HashMap<String, Base> = linkedMapOf()

                val paramList = mutableListOf<TempName>()
                for (param: Node in funcParams.children) {
                    val paramId = param.children[0].t!!.lexeme
                    val paramType = param.children[1].t!!.lexeme
                    val paramDimList = param.children[2]

                    val typeDim = TempName(paramType)
                    for (dim: Node in paramDimList.children) {
                        typeDim.dim.add(dim.t!!.lexeme)
                    }
                    paramList.add(typeDim)
                }


                scope[funcId] = Function(
                    funcId,
                    EntryType2.FUNCTION,
                    innerScope,
                    visibility,
                    returnType,
                    paramList)
            }
            NodeLabel.STRUCTVARDECL.toString() -> {

            }
        }
    }
    fun dfs(scope: HashMap<String, Base> = table, padding: String = "|") {
        if (scope.isEmpty())
            return
        for ((key, value) in scope) {
            println("$padding    $value")
            dfs(value.innerTable, "$padding    ")

        }
    }
}

open class Base(val name: String, val type: EntryType2, val innerTable: HashMap<String, Base>)
class Class(name: String, type: EntryType2, innerTable: HashMap<String, Base>, val inherits: List<String>): Base(name, type, innerTable) {
    override fun toString(): String {
        return "$type | $name"
    }
}
class Function(name: String, type: EntryType2, innerTable: HashMap<String, Base>, val visibility: String, val returnType: String, val params: List<TempName>): Base(name, type, innerTable) {
    override fun toString(): String {
        return "$type | $name | (${params.joinToString(separator = ", ")}): $returnType | $visibility"
    }
}
class Param (name: String, type: EntryType2, innerTable: HashMap<String, Base>, val dataType: String, val dimList: List<String>): Base(name, type, innerTable)

data class TempName (val type: String, val dim: MutableList<String> = mutableListOf()) {
    override fun toString(): String {
        if (dim.isNotEmpty()) {
            var ret = "$type["
            ret += dim.joinToString(separator = "][")
            ret += "]"
            return ret
        }
        else {
            return type
        }

    }
}

enum class EntryType2 {
    INHERIT,
    CLASS,
    FUNCTION,
    DATA,
    PARAM,
    LOCAL;

    override fun toString(): String {
        return super.toString().lowercase(Locale.getDefault())
    }
}

