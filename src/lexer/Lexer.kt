package lexer

import java.io.BufferedReader
import java.io.File

class Lexer(f: File) {
    private var line = 1
    private val r = BufferedReader(f.reader())

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
                ':' -> return Token(TokenType.COLON, line)
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

                            return Token(TokenType.BLOCKCMT, lineStart, cmt)
                        }
                        else -> return Token(TokenType.DIV, line)
                    }
                }
                in 'a'..'z', in 'A'..'Z', '_' -> {
                    var id = c.toString()
                    // first letter indicated an id. keep reading alphanumerics
                    while (true) {
                        if (!peek().isLetterOrDigit() && peek() != '_')
                            break
                        val t = poll()
                        id += t
                    }
                    // reserved words are special cases for id, check each before defaulting to id
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
                        else -> {
                            if (id[0] == '_')
                                return Token(TokenType.INVALIDID, line, id)
                            else
                                return Token(TokenType.ID, line, id)
                        }
                    }
                }
                in '0'..'9' -> {
                    var n = c.toString()
                    var valid = true
                    var hasDecimal = false
                    var hasE = false
                    var zeroStart = false

                    if (c == '0')
                        zeroStart = true

                    while (true) {
                        if (peek().isDigit()) {
                            // a number starting with 0 is only valid with a decimal following
                            if (zeroStart && !hasDecimal)
                                valid = false
                            n += poll()
                        }
                        else if (peek() == 'e') {
                            n += poll()
                            // only 1 'e' is allowed
                            if (hasE || (hasDecimal && n.last() == '0'))
                                valid = false

                            if (!hasE)
                                hasE = true
                            else
                                valid = false

                            // e can be followed by either a sign or a non-zero digit (implies positive)
                            if (peek() == '+' || peek() == '-')
                                n += poll()
                            else if (peek().isDigit()) {
                                // e can be followed by a non-zero integer implying positive
                                val t = poll()
                                n += t
                                if (t == '0') {
                                    valid = false
                                }
                            }
                            else
                                valid = false
                        }
                        else if (peek().isLetter()) {
                            // any letter not 'e' is invalid in a number
                            n += poll()
                            valid = false
                        }
                        else if (peek() == '.') {
                            n += poll()

                            // can only have one decimal
                            if (!hasDecimal)
                                hasDecimal = true
                            else
                                valid = false
                        }
                        else
                            break

                    }

                    // floats not in scientific notation cannot end in 0. ex: 1.20 should be 1.2
                    if (n.last() == '0' && !hasE && hasDecimal)
                        valid = false

                    if (!valid)
                        return Token(TokenType.INVALIDNUM, line, n)
                    else if (hasDecimal || hasE)
                        return Token(TokenType.FLOATNUM, line, n)
                    else
                        return Token(TokenType.INTNUM, line, n)

                }
                else -> {
                    return if (c == '\uFFFF')
                        Token(TokenType.EOF, line)
                    else
                        Token(TokenType.INVALIDCHAR, line, c.toString())
                }
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
        if (t == '\r' && peek() == '\n') { // windows
            poll()
            return true
        }
        else if (t == '\r' || t == '\n') // linux, mac, etc
            return true
        else
            return false
    }

}