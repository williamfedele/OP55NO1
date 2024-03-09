package ast

/**
 * Maps semantic actions to a triple.
 * The triple contains:
 *      an AST operation,
 *      a label for the encapsulated expression,
 *      a number of nodes to pop from the stack for the action (only used for makeFamily action)
 */
class Semantic {
    companion object {
        val actions: Map<String, Triple<String, String, Int>> = mapOf(
            "A1" to Triple("makeNode", "", -1),
            "A3" to Triple("makeFamilyUntilNull", "DIMLIST", -1),
            "A4" to Triple("makeFamily", "VARDECL", 3),
            "A5" to Triple("makeFamilyUntilNull", "PROG", -1),
            "A6" to Triple("makeFamilyUntilNull", "INHERITS", -1),
            "A7" to Triple("makeFamilyUntilNull", "STRUCT", -1),
            "A8" to Triple("makeFamilyUntilNull", "FPARAMS", -1),
            "A9" to Triple("makeFamily", "FUNCHEAD", 3),
            "A10" to Triple("makeFamily", "ASSIGNSTAT", 2),
            "A11" to Triple("makeFamilyUntilNull", "FUNCBODY", -1),
            "A12" to Triple("makeFamily", "ADDOP", 2),
            "A13" to Triple("makeFamily", "MULTOP", 2),
            "A14" to Triple("makeFamily", "WRITE", 1),
            "A15" to Triple("makeFamily", "RETURN", 1),
            "A16" to Triple("makeFamilyUntilNull", "INDICE", -1),
            "A17" to Triple("makeFamilyUntilNull", "APARAMS", -1),
            "A18" to Triple("makeFamily", "READ", 1),
            "A19" to Triple("makeFamily", "VARIABLE", 2),
            "A20" to Triple("makeFamily", "FPARAM", 3),
            "A21" to Triple("makeFamily", "FUNCDEF", 2),
            "A22" to Triple("makeFamilyUntilNull", "IMPLDEF", -1),
            "A23" to Triple("makeFamily", "DOT", 2),
            "A24" to Triple("makeFamily", "FUNCCALL", 2),
            "A25" to Triple("makeFamily", "RELEXPR", 2),
            "A26" to Triple("makeFamilyUntilNull", "STATBLOCK", -1),
            "A27" to Triple("makeFamily", "IF", 3),
            "A28" to Triple("makeFamily", "WHILE", 2),
            "A29" to Triple("makeFamily", "TERM", 1),
            "A30" to Triple("makeFamily", "EXPR", 1),
            "A31" to Triple("makeFamily", "RELOP", 2),
            "A32" to Triple("makeFamily", "ARITHEXPR", 1),
            "A33" to Triple("makeFamily", "NOT", 1),
            "A34" to Triple("makeSign", "", 2),

            "E" to Triple("makeNull", "", -1),
            "B" to Triple("makeEmpty", "EMPTY", -1),

        )

    }
}