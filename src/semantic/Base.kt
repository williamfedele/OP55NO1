package semantic

open class Base(val name: String, val type: Kind, val innerTable: HashMap<String, Base>)
class Class(name: String, type: Kind, innerTable: HashMap<String, Base>, val inherits: List<String>): Base(name, type, innerTable) {
    override fun toString(): String {
        return "$type | $name : ${inherits.joinToString(", ")}"
    }
}
class Function(name: String, type: Kind, innerTable: HashMap<String, Base>, val visibility: String, val returnType: String, val params: List<Variable>): Base(name, type, innerTable) {
    override fun toString(): String {
        return "$type | $name | (${params.joinToString(", ")}): $returnType | $visibility"
    }
}
class Param (name: String, type: Kind, innerTable: HashMap<String, Base>, val variable: Variable): Base(name, type, innerTable) {
    override fun toString(): String {
        return "$type | $name | $variable"
    }
}

class StructVarDecl (name: String, type: Kind, innerTable: HashMap<String, Base>, val variable: Variable, val visibility: String): Base(name, type, innerTable) {
    override fun toString(): String {
        return "$type | $name | $variable | $visibility"
    }
}

class FuncVarDecl (name: String, type: Kind, innerTable: HashMap<String, Base>, val variable: Variable): Base(name, type, innerTable) {
    override fun toString(): String {
        return "$type | $name | $variable"
    }
}