package semantic

import ast.Node
import ast.NodeLabel
import java.io.File
import java.io.FileWriter

class SymTabCreationVisitor (
    val outputSymbolTables: File,
    val outputSemanticErrors: File
): Visitor {

    fun visitAndPrint(node: Node) {
        FileWriter(outputSemanticErrors).use { out -> out.write("") }
        visit(node)
        FileWriter(outputSymbolTables).use { out ->
            out.write("|    ${node.symTab}\n")
            out.write("|    =============================================================\n")
        }
        recurPrint(node.symTab!!)
    }
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
                val structIdToken = node.children[0].t!!
                val id = structIdToken.lexeme
                val inheritList = node.children[1]
                val structdecls = node.children[2]

                if (parentSymTab!!.checkClassDuplicate(id)) {
                    writeError("Error. Multiply defined class $id on line ${structIdToken.line}.")
                }

                // create new symtab for struct linking to the parent
                node.symTab = SymTab(id, parentSymTab)
                // add this struct to parent symtab
                parentSymTab.entries.add(SymTabEntry(id, EntryType.CLASS, node.symTab))

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

                node.symTab = SymTab(id, parentSymTab)
                val functionEntry = SymTabEntry(id, EntryType.FUNCTION, node.symTab)

                //visibility
                functionEntry.visibility = visibility

                //return type
                functionEntry.returnType = returnType

                for (param: Node in fparams.children) {
                    val paramId = param.children[0].t!!.lexeme
                    val paramType = param.children[1].t!!.lexeme
                    val paramDimList = param.children[2]

                    val paramEntry = SymTabEntry(paramId, EntryType.PARAM)
                    paramEntry.dataType = paramType

                    val typeAndDim = TypeAndDim(paramType)
                    for (dim: Node in paramDimList.children) {
                        paramEntry.dimList.add(dim.t!!.lexeme)
                        typeAndDim.dim.add(dim.t!!.lexeme)
                    }
                    functionEntry.typeAndDim.add(typeAndDim)

                    node.symTab!!.addEntry(paramEntry)
                }
                parentSymTab!!.entries.add(functionEntry)
            }
            NodeLabel.STRUCTVARDECL.toString() -> {
                val parentSymTab = node.symTab
                node.symTab = null
                val visibility = node.children[0].t!!.lexeme
                val varIdToken = node.children[1].t!!
                val id = varIdToken.lexeme
                val dataType = node.children[2].t!!.lexeme
                val dataTypeDimList = node.children[3]

                if (parentSymTab!!.checkIdentifierDuplicate(id, EntryType.DATA)) {
                    writeError("Error. Multiply defined identifier in function: $id on line ${varIdToken.line}.")
                }

                val varDecl = SymTabEntry(id, EntryType.DATA)

                varDecl.dataType = dataType
                for (dim: Node in dataTypeDimList.children) {
                    varDecl.dimList.add(dim.t!!.lexeme)
                }
                varDecl.visibility = visibility

                parentSymTab!!.entries.add(varDecl)
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

                node.symTab = SymTab(funcId, parentSymTab)
                val functionEntry = SymTabEntry(funcId, EntryType.FUNCTION, node.symTab)
                functionEntry.returnType = funcReturn

                for (param: Node in funcParams.children) {
                    val paramId = param.children[0].t!!.lexeme
                    val paramType = param.children[1].t!!.lexeme
                    val paramDimList = param.children[2]

                    val paramEntry = SymTabEntry(paramId, EntryType.PARAM)
                    paramEntry.dataType = paramType

                    val typeAndDim = TypeAndDim(paramType)
                    for (dim: Node in paramDimList.children) {
                        val dimValue = if (dim.name == NodeLabel.EMPTY.toString())
                            ""
                        else
                            dim.t!!.lexeme

                        paramEntry.dimList.add(dimValue)
                        typeAndDim.dim.add(dimValue)
                    }
                    functionEntry.typeAndDim.add(typeAndDim)
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
                            val varIdToken = child.children[0].t!!
                            val varId = varIdToken.lexeme
                            val varType = child.children[1].t!!.lexeme
                            val varTypeDimList = child.children[2]

                            if (parentSymTab!!.checkIdentifierDuplicate(varId, EntryType.LOCAL)) {
                                writeError("Error. Multiply defined identifier in function: $varId on line ${varIdToken.line}.")
                            }

                            val varDecl = SymTabEntry(varId, EntryType.LOCAL)
                            varDecl.dataType = varType
                            for (dim: Node in varTypeDimList.children) {
                                varDecl.dimList.add(dim.t!!.lexeme)
                            }
                            parentSymTab.entries.add(varDecl)
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
    fun recurPrint(rootSymTab: SymTab, padding: String = "|") {
        for (entry : SymTabEntry in rootSymTab.entries) {
            FileWriter(outputSymbolTables,true).use { out -> out.write("$padding    $entry\n")}
            if (entry.type == EntryType.CLASS)
                FileWriter(outputSymbolTables,true).use { out -> out.write("$padding    =============================================================\n")}
            if (entry.innerSymTab != null) {
                FileWriter(outputSymbolTables,true).use { out -> out.write("$padding        ${entry.innerSymTab}\n")}
                FileWriter(outputSymbolTables,true).use { out -> out.write("$padding        =============================================================\n")}
                recurPrint(entry.innerSymTab, "$padding    ")
            }

        }
    }

    private fun writeError(s: String) {
        FileWriter(outputSemanticErrors, true).use { out -> out.write("$s\n") }
    }
}

