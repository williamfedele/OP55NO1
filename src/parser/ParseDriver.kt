package parser

import java.io.File

const val GRAMMAR_DIR = "src/parser/grammar/"
const val INPUT_DIR = "src/parser/input/"

fun main() {

    val files = listOf(
        File("${INPUT_DIR}bubblesort.src"),
        File("${INPUT_DIR}polynomial.src")
    )

    val parser = Parser (
        srcFile = files[1],
        tableFileLL1 = File("${GRAMMAR_DIR}ll1.csv"),
        firstFollowSetFile = File("${GRAMMAR_DIR}ll1ff.csv")
    )

    if(parser.parse())
        println("The file is valid.")
    else
        println("Errors were detected.")

}




