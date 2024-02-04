package lexer

enum class TokenType (val repr: String) {
    // Single character
    SEMI("semi"), COMMA("comma"), DOT("dot"), OPENPAR("lpar"), CLOSEPAR("rpar"), OPENCUBR("lcurbr"), CLOSECUBR("rcurbr"), OPENSQBR("lsqbr"), CLOSESQBR("rsqbr"), OR("or"), AND("and"), NOT("not"), MULT("mult"), PLUS("plus"),

    // Requires lookahead
    LT("lt"), GT("gt"), MINUS("minus"),  DIV("div"), ASSIGN("equal"), COLON("colon"),

    // Two characters
    EQ("eq"), NOTEQ("neq"), LEQ("leq"), GEQ("geq"), COLONCOLON("::"), ARROW("arrow"),

    // Reserved words
    VAR("var"), INT("integer"), FLOAT("float"), IF("if"), THEN("then"), ELSE("else"), VOID("void"), PUBLIC("public"), PRIVATE("private"), FUNC("func"), STRUCT("struct"), WHILE("while"), READ("read"), WRITE("write"), RETURN("return"), SELF("self"), INHERITS("inherits"), LET("let"), IMPL("impl"),

    // Types
    ID("id"), FLOATNUM("floatlit"), INTNUM("intlit"), INLINECMT("inlinecmt"), BLOCKCMT("blockcmt"),

    // Error types
    INVALIDCHAR("invalidchar"), INVALIDNUM("invalidnum"), INVALIDID("invalidid"),

    // Used to signal end of file. Discarded upon reading from lexer.
    EOF("");

    private fun getErrorTypes(): List<TokenType> {
        return listOf(INVALIDCHAR, INVALIDNUM, INVALIDID)
    }
    fun isError(): Boolean {
        return this in getErrorTypes()
    }

}