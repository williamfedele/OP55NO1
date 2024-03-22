package semantic

class SymTab(name:String, parent: SymTab? = null) {
    val name = name
    var parentTable = parent
    var entries = mutableListOf<SymTabEntry>()

    fun addEntry(entry: SymTabEntry) {
        entries.add(entry)
    }

    // called from the global symtab when analyzing an impl
    // finds the class with name className
    // finds the function with name funcName
    fun getFuncSymTab(className: String, funcName: String, type: EntryType): SymTab? {
        val globalSymTab = getGlobalSymTab()

        for (classEntry: SymTabEntry in globalSymTab.entries) {
            if(classEntry.name == className && classEntry.type == EntryType.CLASS) {
                if(classEntry.innerSymTab == null)
                    break
                for (funcEntry: SymTabEntry in classEntry.innerSymTab.entries) {
                    if (funcEntry.name == funcName && funcEntry.type == type) {
                        return funcEntry.innerSymTab
                    }
                }
            }
        }
        return null
    }

    fun checkClassDuplicate(className: String): Boolean {
        val globalSymTab = getGlobalSymTab()
        for (classEntry: SymTabEntry in globalSymTab.entries) {
            if(classEntry.name == className && classEntry.type == EntryType.CLASS)
                return true
        }
        return false
    }

    fun checkIdentifierDuplicate(id: String, type: EntryType): Boolean {
        for (local: SymTabEntry in this.entries) {
            if (local.name == id && local.type == type)
                return true
        }
        return false
    }

    private fun getGlobalSymTab(): SymTab {
        var symTabNext: SymTab? = this
        var global: SymTab = this
        while (symTabNext != null) {
            global = symTabNext
            symTabNext = symTabNext.parentTable
        }
        return global
    }

    override fun toString(): String {
        if (parentTable != null && parentTable!!.name != "global")
            return "table: ${parentTable!!.name}::$name"
        else
            return "table: $name"
    }
}