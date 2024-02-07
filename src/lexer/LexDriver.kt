package lexer

import java.io.File

fun main () {
    val inputPath = "src/lexer/input/"
    val outputPath = "src/lexer/output/"
    val files = listOf("lexmixedgrading", "lexpositivegrading", "lexnegativegrading")
    val fileEnding = ".src"

    for (file :String in files) {

        // extract all tokens from the current file
        val lex = Lexer(File(inputPath+file+fileEnding))
        val tokens = ArrayList<Token>()
        var token = lex.nextToken()
        while(token.type != TokenType.EOF) {
            tokens.add(token)
            token = lex.nextToken()
        }

        // print the extracted tokens
        // file extensions:
        //      .outlextokens = all tokens
        //      .outlexerrors = only errors in a more human-readable way
        var line = tokens[0].line

        val outputTokenFile = File("$outputPath$file.outlextokens")
        val outputErrorFile = File("$outputPath$file.outlexerrors")
        val errors = tokens.filter { it.type.isError() }

        outputTokenFile.printWriter().use { out ->
            tokens.forEach {
                if(it.line != line) {
                    line = it.line
                    out.println()
                }
                out.print(it)
            }
        }
        outputErrorFile.printWriter().use { out ->
            errors.forEach {
                out.println(it.getErrorMessage())
            }
        }

    }
}