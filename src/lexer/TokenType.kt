package lexer

enum class TokenType (val repr: String) {
    // Single character
    SEMI(";"), COMMA(","), DOT("."), OPENPAR("("), CLOSEPAR(")"), OPENCUBR("{"), CLOSECUBR("}"), OPENSQBR("["), CLOSESQBR("]"), OR("|"), AND("&"), NOT("!"), MULT("*"), PLUS("+"),

    // Requires lookahead
    LT("<"), GT(">"), MINUS("-"),  DIV("/"), ASSIGN("="), COLON(":"),

    // Two characters
    EQ("=="), NOTEQ("<>"), LEQ("<="), GEQ(">="), COLONCOLON("::"), ARROW("->"),

    // Reserved words
    VAR("var"), INT("integer"), FLOAT("float"), IF("if"), THEN("then"), ELSE("else"), VOID("void"), PUBLIC("public"), PRIVATE("private"), FUNC("func"), STRUCT("struct"), WHILE("while"), READ("read"), WRITE("write"), RETURN("return"), SELF("self"), INHERITS("inherits"), LET("let"), IMPL("impl"),

    // Types
    ID("id"), FLOATNUM("floatnum"), INTNUM("intnum"), INLINECMT("inlinecmt"), BLOCKCMT("blockcmt"),

    // Error types
    INVALIDCHAR("invalidchar"), INVALIDNUM("invalidnum"), INVALIDID("invalidid"),

    // Used to signal end of file. Discarded upon reading from lexer.
    EOF("");

    fun getErrorTypes(): List<TokenType> {
        return listOf(INVALIDCHAR, INVALIDNUM, INVALIDID)
    }
    fun isError(): Boolean {
        return this in getErrorTypes()
    }


}