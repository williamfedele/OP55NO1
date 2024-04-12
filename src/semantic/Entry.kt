package semantic

/**
 * Contains different types of symbol table entries in an inheritance structure
 */
open class Entry(val name: String, val innerTable: HashMap<String, Entry>? = null)
class Class(name: String, innerTable: HashMap<String, Entry>, val inherits: List<String>, var memSize: Int = 0): Entry(name, innerTable) {
    override fun toString(): String {
        return "class | $name : ${inherits.joinToString(", ")} | Size: $memSize"
    }
}
class Function(name: String, innerTable: HashMap<String, Entry>, val parentClass: String?, val visibility: String, val returnType: String, val params: List<Variable>, var moonLabel: String = "", var moonReturnLabel: String = ""): Entry(name, innerTable) {
    override fun toString(): String {
        return "function | $name | (${params.joinToString(", ")}): $returnType | $visibility | $moonLabel"
    }
}
class Param (name: String, val variable: Variable, var moonVarName: String = ""): Entry(name) {
    override fun toString(): String {
        return "param | $name | $variable | $moonVarName"
    }
}

class Data (name: String, val variable: Variable, val visibility: String, var memSize: Int = 0, var memOffset: Int = 0): Entry(name) {
    override fun toString(): String {
        return "data | $name | $variable | $visibility | Size: $memSize | Offset: $memOffset"
    }
}

class Local (name: String, val variable: Variable): Entry(name) {
    override fun toString(): String {
        return "local | $name | $variable"
    }
}
