package semantic

import java.util.*

class SymTabEntry (val name: String, val type: EntryType, inner: SymTab? = null) {

    val innerSymTab = inner // used for inner scopes
    var inheritList = mutableListOf<String>()
    var visibility = ""

    // used for variables
    var dataType = ""
    var dimList = mutableListOf<String>()

    // used for holding function symtab entries to keep associated dimlist with variable type
    var typeAndDim = mutableListOf<TypeAndDim>()

    // used for functions
    var inputType = mutableListOf<String>()
    var returnType = ""

    override fun toString(): String {
        return when (type) {
            EntryType.FUNCTION -> {
                // TODO
                // need to add dimlist to function params. one param can have multiple. not tracking that right now
                // need to pull out inputType + inputDimList to a wrapper class to keep them together
                "$type | $name | (${typeAndDim.joinToString(separator = ", ")}): $returnType | $visibility"
            }
            EntryType.INHERIT -> "$type | ${inheritList.joinToString(separator = ", ")}"
            EntryType.CLASS -> "$type | $name"
            EntryType.PARAM,
            EntryType.LOCAL -> {
                if (dimList.isEmpty())
                    "$type | $name | $dataType"
                else {
                    "$type | $name | $dataType[${dimList.joinToString(separator = "][")}]"
                }
            }
            EntryType.DATA -> "$type | $name | $dataType | $visibility"
        }
    }
}

enum class EntryType {
    INHERIT,
    CLASS,
    FUNCTION,
    DATA,
    PARAM,
    LOCAL;

    override fun toString(): String {
        return super.toString().lowercase(Locale.getDefault())
    }
}

data class TypeAndDim (val type: String, val dim: MutableList<String> = mutableListOf()) {
    override fun toString(): String {
        if (dim.isNotEmpty()) {
            var ret = "$type["
            ret += dim.joinToString(separator = "][")
            ret += "]"
            return ret
        }
        else {
            return type
        }

    }
}
