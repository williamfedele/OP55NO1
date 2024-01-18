package lexer

import java.io.Reader
import kotlin.text.substring

class Lexer(val r: Reader) {
    private var line = 1

    fun nextToken(): Token {
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
                r.mark(1)
                var temp = r.read().toChar()
                if (temp == '/') {
                    // inlinecmt
                    var acc : String = next.toChar().toString()
                    while (temp != '\n') {
                        acc += temp
                        temp = r.read().toChar()
                    }

                    return Token(TokenType.INLINECMT, line++, acc)

                }
                else if (temp == '*') {
                    // block comment
                    var acc : String = next.toChar().toString()
                    acc += temp
                    var temp2 = r.read()
                    var counter = 1
                    while (temp2 != -1) {
                        if (temp2.toChar() == '\n') {
                            acc += "\\n"
                        }
                        else
                            acc += temp2.toChar()
                        if (acc[acc.length-2].toString() + acc[acc.length-1].toString() == "*/") {
                            counter--
                            if (counter == 0)
                                break
                        }
                        if (acc[acc.length-2].toString() + acc[acc.length-1].toString() == "/*")
                            counter++

                        temp2 = r.read()
                    }
                    return if (counter != 0) {
                        // TODO handle error, block comment wasnt closed
                        Token(TokenType.EOF, line)
                    } else
                        Token(TokenType.BLOCKCMT, line, acc)
                }
                else {
                    r.reset()
                    return Token(TokenType.DIV, line)
                }
            }
            '-' ->  {
                // check if - or ->
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
                // check if = or ==
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
                // check if < or <> or <=
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
                // check if > or >=
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
                /*
                If the first token is a letter
                Continue reading while a letter or number is found. Else (symbol, whitespace) stop and assess.
                Check if it matches a reserved word
                If not, then it must be an id (mix of letters/numbers, starting with letter)
                */

                var acc : String = ""
                // TODO check what characters are valid in an ID, added underscore below
                while(next.toChar().isLetterOrDigit() || next.toChar() == '_') {
                    //build string
                    acc += next.toChar()

                    //mark position incase non letter/digit found to rollback
                    r.mark(1)

                    //get next char
                    next = r.read()
                }
                r.reset()

                when (acc.lowercase()) {
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
                    else -> return Token(TokenType.ID, line, acc)
                }
            }
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
}