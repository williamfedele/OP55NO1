package lexer

import java.io.Reader

class Lexer(val r: Reader) {
    private var line = 1

    fun nextToken(): Token {
        var next = r.read()

        // skip whitespace until non whitespace found
        while(next.toChar().isWhitespace()) {
            // increment line tracker
            if(next.toChar() == '\n')
                line++

            next = r.read()
        }

        if(next == -1)
            return Token(TokenType.EOF,0)

        when (next.toChar()) {
            '+' -> return Token(TokenType.PLUS, line)
            '*' -> return Token(TokenType.MULT, line)
            '|' -> return Token(TokenType.OR, line)
            '&' -> return Token(TokenType.AND, line)
            '!' -> return Token(TokenType.NOT, line)
            '(' -> return Token(TokenType.OPENPAR, line)
            ')' -> return Token(TokenType.CLOSEPAR, line)
            '{' -> return Token(TokenType.OPENCUBR, line)
            '}' -> return Token(TokenType.CLOSECUBR, line)
            '[' -> return Token(TokenType.OPENSQBR, line)
            ']' -> return Token(TokenType.CLOSESQBR, line)
            ',' -> return Token(TokenType.COMMA, line)
            '.' -> return Token(TokenType.DOT, line)
            ';' -> return Token(TokenType.SEMI, line)
            ':' -> {
                r.mark(1)
                val temp = r.read().toChar()
                if(temp == ':')
                    return Token(TokenType.COLONCOLON, line)
                else {
                    r.reset()
                    return Token(TokenType.COLON, line)
                }
            }
            '/' -> {
                // TODO if there are two slashes, create an inlinecmt
                return Token(TokenType.DIV, line)
            }
            '-' ->  {
                r.mark(1)
                val temp = r.read().toChar()
                if(temp == '>')
                    return Token(TokenType.ARROW, line)
                else {
                    r.reset()
                    return Token(TokenType.MINUS, line)
                }
            }
            '=' -> {
                r.mark(1000)
                val temp = r.read().toChar()
                if (temp == '=')
                    return Token(TokenType.EQ, line)
                else {
                    r.reset()
                    return Token(TokenType.ASSIGN, line)
                }
            }
            '<' -> {
                r.mark(1)
                val temp = r.read().toChar()
                if (temp == '>')
                    return Token(TokenType.NOTEQ, line)
                else if (temp == '=')
                    return Token(TokenType.LEQ, line)
                else {
                    r.reset()
                    return Token(TokenType.LT, line)
                }
            }
            '>' -> {
                r.mark(1)
                val temp = r.read().toChar()
                if (temp == '=')
                    return Token(TokenType.GEQ, line)
                else {
                    r.reset()
                    return Token(TokenType.GT, line)
                }
            }
            in 'a'..'z' -> {
                //TODO skipping letters for now
                // if a letter is detected, should read until whitespace and evaluate if its a reserved word or id
                while(next.toChar().isLetter())
                    next = r.read()
                return Token(TokenType.EOF, line)
            }
            else -> {

                return Token(TokenType.EOF, line)
            }

        }
    }
}