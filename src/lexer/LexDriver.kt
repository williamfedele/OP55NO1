package lexer

import java.io.BufferedReader
import java.io.File

enum class Test(val v: String) {
    TEST("abc")
}

fun main () {
    val file1 = File("src/lexer/testfiles/lexpositivegrading.src")
    val file2 = File("src/lexer/testfiles/lexnegativegrading.src")

    val reader = BufferedReader(file1.reader())

    val lex = Lexer(reader)
    val tokens = ArrayList<Token>()
    var token = lex.nextToken()
    while(token.type != TokenType.EOF) {
        token = lex.nextToken()
        tokens.add(token)
    }

    //print the tokens
    var line = tokens[0].line
    for (t : Token in tokens) {
        // newline if the token appeared on a new line
        if(t.line != line) {
            line = t.line
            println()
        }
        print(t)
    }

}