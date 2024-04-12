package generation

import ast.Node
import ast.NodeLabel
import semantic.Class
import semantic.Data
import semantic.Entry

class AllocationCalculator (val global: HashMap<String, Entry>) {


    fun traverse(node: Node?, structName: String = "") {
        if (node == null)
            return
        when (node.name) {
            NodeLabel.PROG.toString() -> {
                for (child in node.children)
                    traverse(child)
            }
            NodeLabel.STRUCT.toString() -> {
                val structId = node.children[0].t!!.lexeme
                val inheritList = node.children[1].children
                val structDecls = node.children[2].children
                for (child in structDecls)
                    traverse(child, structId)

                val classScope = global[structId] as Class
                for (inherit in inheritList) {
                    val inheritName = inherit.t!!.lexeme
                    val inheritScope = global[inheritName] as Class
                    classScope.memSize += inheritScope.memSize
                }

                // calculate offsets
                if (classScope.innerTable != null){
                    var startPos = 0
                    for (dataMember in classScope.innerTable) {
                        val varScope = dataMember.value as Data
                        varScope.memOffset = startPos
                        startPos += varScope.memSize
                    }
                }

            }
            NodeLabel.STRUCTDECLS.toString() -> {
                for (child in node.children)
                    traverse(child, structName)
            }
            NodeLabel.STRUCTVARDECL.toString() -> {
                val varId = node.children[1].t!!.lexeme
                val varType = node.children[2]
                val dimList = node.children[3].children
                val varScope = global[structName]!!.innerTable?.get(varId) as Data

                val arrSize = Util.processDimList(dimList)

                when(varType.name) {
                    "FLOAT" -> {
                        varScope.memSize = arrSize * FLOAT_SIZE
                        //classScope.memSize += arrSize * FLOAT_SIZE
                    }
                    "INTEGER" -> {
                        varScope.memSize = arrSize * INT_SIZE
                    }
                    else -> {println("unhandled vardecl type in allocation calculator.")}
                }
            }
        }
    }
}