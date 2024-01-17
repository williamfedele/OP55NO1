package lexer

class Token(val type: TokenType, val line: Int, val lexeme: String = type.repr) {
    override fun toString(): String {
        return "[${type.toString().lowercase()}, $lexeme, $line]"

    }
}