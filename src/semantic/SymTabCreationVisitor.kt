package semantic

import ast.Node
import ast.NodeLabel

class SymTabCreationVisitor: Visitor {
    override fun visit(node: Node) {
        when (node.name) {
            // PROG contains STRUCT, IMPL, FUNC
            NodeLabel.PROG.toString() -> {
                node.symTab = SymTab("global")
                for (child: Node in node.children) {
                    child.symTab = node.symTab
                    child.accept(this)
                }
            }
            NodeLabel.STRUCT.toString() -> {
                val parentSymTab = node.symTab
                val id = node.children[0].t!!.lexeme
                val inheritList = node.children[1]
                val structdecls = node.children[2]

                // create new symtab for struct linking to the parent
                node.symTab = SymTab(id)
                // add this struct to parent symtab
                parentSymTab!!.entries.add(SymTabEntry(id, EntryType.CLASS, node.symTab))

                // build inherit entry
                val inheritEntry = SymTabEntry("inherits", EntryType.INHERIT)
                for (child: Node in inheritList.children) {
                    inheritEntry.inheritList.add(child.t!!.lexeme)
                }
                // add to this struct symtab
                node.symTab!!.entries.add(inheritEntry)

                for (child: Node in structdecls.children) {
                    child.symTab = node.symTab
                    child.accept(this)
                }

            }
            NodeLabel.STRUCTFUNCHEAD.toString() -> {
                val parentSymTab = node.symTab
                val visibility = node.children[0].t!!.lexeme
                val id = node.children[1].t!!.lexeme
                val fparams = node.children[2]
                val returnType = node.children[3].t!!.lexeme

                node.symTab = SymTab(id)
                val functionEntry = SymTabEntry(id, EntryType.FUNCTION, node.symTab)
                node.symTab!!.parentTable = parentSymTab
                parentSymTab!!.entries.add(functionEntry)

                //visibility
                functionEntry.visibility = visibility

                //return type
                functionEntry.returnType = returnType

                for (param: Node in fparams.children) {
                    val paramId = param.children[0].t!!.lexeme
                    val paramType = param.children[1].t!!.lexeme
                    val paramDimList = param.children[2]

                    functionEntry.inputType.add(paramType)

                    val paramEntry = SymTabEntry(paramId, EntryType.PARAM)
                    paramEntry.dataType = paramType
                    for (dim: Node in paramDimList.children) {
                        paramEntry.dimList.add(dim.t!!.lexeme)
                        functionEntry.dimList.add(dim.t!!.lexeme)
                    }

                    node.symTab!!.addEntry(paramEntry)
                }

            }
            NodeLabel.STRUCTVARDECL.toString() -> {
                val parentSymTab = node.symTab
                val visibility = node.children[0].t!!.lexeme
                val id = node.children[1].t!!.lexeme
                val dataType = node.children[2].t!!.lexeme
                val dataTypeDimList = node.children[3]

                node.symTab = null
                val varDecl = SymTabEntry(id, EntryType.DATA)
                parentSymTab!!.entries.add(varDecl)

                varDecl.dataType = dataType
                for (dim: Node in dataTypeDimList.children) {
                    varDecl.dimList.add(dim.t!!.lexeme)
                }
                varDecl.visibility = visibility
            }
            NodeLabel.IMPLDEF.toString() -> {
                val parentSymTab = node.symTab
                val implId = node.children[0].t!!.lexeme
                val implDefs = node.children[1]

                for (funcDef: Node in implDefs.children) {
                    val funcHead = funcDef.children[0] // TODO, validate impl header matches struct declaration for errors

                    val funcId = funcHead.children[0].t!!.lexeme
                    val funcParams = funcHead.children[1]
                    val funcRet = funcHead.children[2].t!!.lexeme

                    // locate the related class function declared previously with a struct def
                    // both impl and func must match
                    val relSymTab = parentSymTab!!.getFuncSymTab(implId, funcId, EntryType.FUNCTION)
                    if(relSymTab == null) {
                        // TODO report error
                        // implementing undefined function
                    }
                    else {
                        val funcBody = funcDef.children[1]
                        funcBody.symTab = relSymTab
                        funcBody.accept(this)
                    }

                }
            }
            NodeLabel.FUNCDEF.toString() -> {
                // used to handle free functions only

                val funcHead = node.children[0]
                funcHead.symTab = node.symTab
                funcHead.accept(this)

                val funcBody = node.children[1]
                funcBody.symTab = funcHead.symTab
                funcBody.accept(this)
            }
            NodeLabel.FUNCHEAD.toString() -> {
                val parentSymTab = node.symTab
                val funcId = node.children[0].t!!.lexeme
                val funcParams = node.children[1]
                val funcReturn = node.children[2].t!!.lexeme

                node.symTab = SymTab(funcId)
                val functionEntry = SymTabEntry(funcId, EntryType.FUNCTION, node.symTab)
                node.symTab!!.parentTable = parentSymTab
                functionEntry.returnType = funcReturn

                for (param: Node in funcParams.children) {
                    val paramId = param.children[0].t!!.lexeme
                    val paramType = param.children[1].t!!.lexeme
                    val paramDimList = param.children[2]

                    functionEntry.inputType.add(paramType)

                    val paramEntry = SymTabEntry(paramId, EntryType.PARAM)
                    paramEntry.dataType = paramType
                    for (dim: Node in paramDimList.children) {
                        val dimValue = if (dim.name == NodeLabel.EMPTY.toString())
                            "0"
                        else
                            dim.t!!.lexeme

                        paramEntry.dimList.add(dimValue)
                        functionEntry.dimList.add(dimValue)
                    }

                    node.symTab!!.addEntry(paramEntry)
                }

                parentSymTab!!.entries.add(functionEntry)
            }
            NodeLabel.FUNCBODY.toString() -> {
                val parentSymTab = node.symTab
                node.symTab = null
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
                            val varId = child.children[0].t!!.lexeme
                            val varType = child.children[1].t!!.lexeme
                            val varTypeDimList = child.children[2]

                            val varDecl = SymTabEntry(varId, EntryType.LOCAL)
                            varDecl.dataType = varType
                            for (dim: Node in varTypeDimList.children) {
                                varDecl.dimList.add(dim.t!!.lexeme)
                            }
                            parentSymTab!!.entries.add(varDecl)
                        }
                        else -> {println("Missing a case for ${child.name} in the FUNCBODY symtab handler.")}
                    }
                }
            }
            else -> {node.children.forEach{child ->
                child.symTab = node.symTab
                child.accept(this)
            }}
        }
    }
}

