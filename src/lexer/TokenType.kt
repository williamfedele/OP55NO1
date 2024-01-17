package lexer

enum class TokenType (val repr: String) {
    EQ("=="), NOTEQ("<>"), LT("<"), GT(">"), LEQ("<="), GEQ(">="), PLUS("+"), MINUS("-"), MULT("*"), DIV("/"), ASSIGN("="),
    OR("|"), AND("&"), NOT("!"),
    OPENPAR("("), CLOSEPAR(")"), OPENCUBR("{"), CLOSECUBR("}"), OPENSQBR("["), CLOSESQBR("]"),
    SEMI(";"), COMMA(","), DOT("."), COLON(":"), COLONCOLON("::"), ARROW("->"), IF("if"), THEN("then"), ELSE("else"), VOID("void"), PUBLIC("public"), PRIVATE("private"), FUNC("func"), STRUCT("struct"), WHILE("while"), READ("read"), WRITE("write"), RETURN("return"), SELF("self"), INHERITS("inherits"), LET("let"), IMPL("impl"),
    EOF("")
}