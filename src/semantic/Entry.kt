package semantic

open class Entry(val name: String, val type: Kind, val innerTable: HashMap<String, Entry>)
class Class(name: String, type: Kind, innerTable: HashMap<String, Entry>, val inherits: List<String>): Entry(name, type, innerTable) {
    override fun toString(): String {
        return "$type | $name : ${inherits.joinToString(", ")}"
    }
}
class Function(name: String, type: Kind, innerTable: HashMap<String, Entry>, val visibility: String, val returnType: String, val params: List<Variable>): Entry(name, type, innerTable) {
    override fun toString(): String {
        return "$type | $name | (${params.joinToString(", ")}): $returnType | $visibility"
    }
}
class Param (name: String, type: Kind, innerTable: HashMap<String, Entry>, val variable: Variable): Entry(name, type, innerTable) {
    override fun toString(): String {
        return "$type | $name | $variable"
    }
}

class StructVarDecl (name: String, type: Kind, innerTable: HashMap<String, Entry>, val variable: Variable, val visibility: String): Entry(name, type, innerTable) {
    override fun toString(): String {
        return "$type | $name | $variable | $visibility"
    }
}

class FuncVarDecl (name: String, type: Kind, innerTable: HashMap<String, Entry>, val variable: Variable): Entry(name, type, innerTable) {
    override fun toString(): String {
        return "$type | $name | $variable"
    }
}