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
        for (classEntry: SymTabEntry in entries) {
            if(classEntry.name == className) {
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

    override fun toString(): String {
        if (parentTable != null)
            return "table: ${parentTable!!.name}::$name"
        else
            return "table: $name"
    }
}