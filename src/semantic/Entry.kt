package semantic

/**
 * Contains different types of symbol table entries in an inheritance structure
 */
open class Entry(val name: String, val innerTable: HashMap<String, Entry>? = null)
class Class(name: String, innerTable: HashMap<String, Entry>, val inherits: List<String>): Entry(name, innerTable) {
    override fun toString(): String {
        return "class | $name : ${inherits.joinToString(", ")}"
    }
}
class Function(name: String, innerTable: HashMap<String, Entry>, val visibility: String, val returnType: String, val params: List<Variable>): Entry(name, innerTable) {
    override fun toString(): String {
        return "function | $name | (${params.joinToString(", ")}): $returnType | $visibility"
    }
}
class Param (name: String, val variable: Variable): Entry(name) {
    override fun toString(): String {
        return "param | $name | $variable"
    }
}

class Data (name: String, val variable: Variable, val visibility: String): Entry(name) {
    override fun toString(): String {
        return "data | $name | $variable | $visibility"
    }
}

class Local (name: String, val variable: Variable): Entry(name) {
    override fun toString(): String {
        return "local | $name | $variable"
    }
}