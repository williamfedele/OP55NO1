package lexer

class Token(val type: TokenType, val line: Int, val lexeme: String = type.repr) {
    override fun toString(): String {
        if (type == TokenType.EOF)
            return ""
        return "[${type.toString().lowercase()}, $lexeme, $line]"

    }
}