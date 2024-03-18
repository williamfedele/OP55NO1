package semantic

class SymTab(name:String, parent: SymTab? = null) {
    val name = name
    val parentTable = parent
}