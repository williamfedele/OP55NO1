package ast

class Semantic {
    companion object {
        val actions: Map<String, Triple<String, String, Int>> = mapOf(
            "A1" to Triple("makeNode", "Id", -1),
            "A2" to Triple("makeNode", "Dim", -1),
            "A3" to Triple("makeFamilyUntilNull", "DimList", -1),
            "A4" to Triple("makeFamily", "VarDecl", 3),
            "A5" to Triple("makeFamilyUntilNull", "Prog", -1),
            "A6" to Triple("makeFamilyUntilNull", "InheritList", -1),
            "A7" to Triple("makeFamilyUntilNull", "Struct", -1),
            "T" to Triple("makeNode", "Type", -1),
            "E" to Triple("makeNull", "", -1),
        )

    }
}