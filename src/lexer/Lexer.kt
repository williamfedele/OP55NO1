package lexer

import java.io.Reader

class Lexer(private val r: Reader) {
    private var line = 1

    fun nextToken(): Token {
        while (true) {
            var c = poll()

            // whitespace is not tokenized, skip until non-whitespace found
            while (c.isWhitespace()) {
                // increment line tracker
                if(c == '\n')
                    line++

                c = poll()
            }

            when (c) {
                '(' -> return Token(TokenType.OPENPAR, line)
                ')' -> return Token(TokenType.CLOSEPAR, line)
                '{' -> return Token(TokenType.OPENCUBR, line)
                '}' -> return Token(TokenType.CLOSECUBR, line)
                '[' -> return Token(TokenType.OPENSQBR, line)
                ']' -> return Token(TokenType.CLOSESQBR, line)
                '+' -> return Token(TokenType.PLUS, line)
                '*' -> return Token(TokenType.MULT, line)
                '|' -> return Token(TokenType.OR, line)
                '&' -> return Token(TokenType.AND, line)
                '!' -> return Token(TokenType.NOT, line)
                ',' -> return Token(TokenType.COMMA, line)
                '.' -> return Token(TokenType.DOT, line)
                ';' -> return Token(TokenType.SEMI, line)
                ':' -> {
                    when (peek()) {
                        ':' -> {
                            poll()
                            return Token(TokenType.COLONCOLON, line)
                        }
                        else ->  return Token(TokenType.COLON, line)
                    }
                }
                '-' -> {
                    when (peek()) {
                        '>' -> {
                            poll()
                            return Token(TokenType.ARROW, line)
                        }
                        else -> return Token(TokenType.MINUS, line)
                    }
                }
                '=' -> {
                    when (peek()) {
                        '=' -> {
                            poll()
                            return Token(TokenType.EQ, line)
                        }
                        else -> return Token(TokenType.ASSIGN, line)
                    }
                }
                '>' -> {
                    when (peek()) {
                        '=' -> {
                            poll()
                            return Token(TokenType.GEQ, line)
                        }
                        else -> return Token(TokenType.GT, line)
                    }
                }
                '<' -> {
                    when (peek()) {
                        '>' -> {
                            poll()
                            return Token(TokenType.NOTEQ, line)
                        }
                        '=' -> {
                            poll()
                            return Token(TokenType.LEQ, line)
                        }
                        else -> return Token(TokenType.LT, line)
                    }
                }
                '/' -> {
                    when (peek()) {
                        '/' -> {
                            poll()
                            var cmt = "//"
                            while (true) {
                                val t = poll()
                                if (isNewline(t))
                                    break
                                cmt += t
                            }
                            return Token(TokenType.INLINECMT, line++, cmt)
                        }
                        '*' -> {
                            poll()
                            var cmt = "/*"
                            val lineStart = line
                            var openCount = 1
                            while (true) {
                                // escape newline
                                val t = poll()
                                if (isNewline(t)) {
                                    cmt += "\\n"
                                    line++
                                }
                                else
                                    cmt += t

                                when (cmt.takeLast(2)) {
                                    "*/" -> {
                                        openCount--
                                        if (openCount == 0)
                                            break
                                    }
                                    "/*" -> {
                                        openCount++
                                    }
                                    else -> continue
                                }
                            }
                            // TODO potential error if EOF reached and block comment wasnt closed
                            return Token(TokenType.BLOCKCMT, lineStart, cmt)
                        }
                        else -> return Token(TokenType.DIV, line)
                    }
                }
                in 'a'..'z' -> {
                    var id = c.toString()
                    while (true) {
                        if (!peek().isLetterOrDigit() && peek() != '_')
                            break
                        val t = poll()
                        id += t
                    }
                    when (id) {
                        "if" -> return Token(TokenType.IF, line)
                        "public" -> return Token(TokenType.PUBLIC, line)
                        "read" -> return Token(TokenType.READ, line)
                        "then" -> return Token(TokenType.THEN, line)
                        "private" -> return Token(TokenType.PRIVATE, line)
                        "write" -> return Token(TokenType.WRITE, line)
                        "else" -> return Token(TokenType.ELSE, line)
                        "func" -> return Token(TokenType.FUNC, line)
                        "return" -> return Token(TokenType.RETURN, line)
                        "integer" -> return Token(TokenType.INT, line)
                        "var" -> return Token(TokenType.VAR, line)
                        "self" -> return Token(TokenType.SELF, line)
                        "float" -> return Token(TokenType.FLOAT, line)
                        "struct" -> return Token(TokenType.STRUCT, line)
                        "inherits" -> return Token(TokenType.INHERITS, line)
                        "void" -> return Token(TokenType.VOID, line)
                        "while" -> return Token(TokenType.WHILE, line)
                        "let" -> return Token(TokenType.LET, line)
                        "impl" -> return Token(TokenType.IMPL, line)
                        else -> return Token(TokenType.ID, line, id)
                    }
                }
                else -> return Token(TokenType.EOF, line)
            }
        }
    }
    fun nextToken1(): Token {
        var next = r.read()

        // whitespace is not tokenized, skip until non-whitespace found
        while(next.toChar().isWhitespace()) {
            // increment line tracker
            if(next.toChar() == '\n')
                line++

            next = r.read()
        }

        if(next == -1)
            return Token(TokenType.EOF,0)

        when (next.toChar()) {

            in '1'..'9' -> {
                /*
                If the first token is non zero int. Continue while ints are found.
                If a dot is found, there must be no trailing zeroes in decimals.
                If ‘e’ is found, next char must be plus or minus, next char must be non zero
                 */
                var temp = next.toChar()
                var acc = ""
                while (!temp.isWhitespace()) {
                    acc += temp
                    temp = r.read().toChar()
                }

                if (temp == '\n')
                    line++
                return Token(TokenType.INTNUM, line, acc)
            }
            '0' -> {
                /*
                If the first token is 0, the next token must be whitespace.
                If not, continue until whitespace and report invalid number.
                */
                r.mark(1)
                var temp = r.read().toChar()
                if(temp.isDigit()) {
                    // TODO numbers cannot start with 0
                    return Token(TokenType.EOF, line)
                }
                else if (temp.isLetter()) {
                    // error, id starting with number
                    
                    var acc = next.toChar().toString()
                    while (!temp.isWhitespace()) {
                        acc += temp
                        temp = r.read().toChar()
                    }
                    return Token(TokenType.INVALIDID, line, acc)
                }
                else if (temp == '.') {
                    // TODO decimal check
                    return Token(TokenType.EOF, line)
                }
                else if (temp.isWhitespace()) {
                    r.reset()
                    return Token(TokenType.INTNUM, line, next.toChar().toString())
                }
                else {
                    return return Token(TokenType.EOF, line)
                }
            }
            else -> {
                // anything else is invalid
                return Token(TokenType.EOF, line)
            }

        }
    }

    // Peeks at the next character without moving position in a file reader
    private fun peek(): Char{
        r.mark(1)
        val next = r.read().toChar()
        r.reset()
        return next
    }

    // Polls the next character in the file reader
    private fun poll(): Char {
        return r.read().toChar()
    }

    // Checks if there is a newline character. Detects CRLF, CR, LF.
    private fun isNewline(t: Char): Boolean {
        if (peek() == '\n') {
            poll()
            return true
        }
        else if (t == '\r' || t == '\n')
            return true
        else
            return false
    }

}