package lexer

class Token(val type: TokenType, val line: Int, val lexeme: String = type.repr) {
    // Token string representation
    override fun toString(): String {
        if (type == TokenType.EOF)
            return ""
        return "[${type.toString().lowercase()}, $lexeme, $line]"
    }
    // Get the associated message for error tokens.
    fun getErrorMessage(): String {
        return when (type) {
            TokenType.INVALIDCHAR -> "Lexical error: Invalid character: \"$lexeme\": line $line."
            TokenType.INVALIDNUM -> "Lexical error: Invalid number: \"$lexeme\": line $line."
            TokenType.INVALIDID -> "Lexical error: Invalid identifier: \"$lexeme\": line $line."
            else -> ""
        }
    }
}