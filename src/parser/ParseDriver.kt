package parser

import java.io.File

const val GRAMMAR_DIR = "src/parser/grammar/"
const val INPUT_DIR = "src/parser/input/"
const val OUTPUT_DIR = "src/parser/output/"

fun main() {

    val files = listOf(
        File("bubblesort.src"),
        File("polynomial.src"),
        File("misc.src")
    )

    for (file: File in files) {
        val parser = Parser (
            srcFile = File("${INPUT_DIR}$file"),
            tableFileLL1 = File("${GRAMMAR_DIR}ll1.csv"),
            firstFollowSetFile = File("${GRAMMAR_DIR}ll1ff.csv"),
            outputDerive = File("$OUTPUT_DIR$file.outderivation"),
            outputSyntaxErrors = File("$OUTPUT_DIR$file.outsyntaxerrors")
        )

        if(parser.parse())
            println("The file is valid.")
        else
            println("Errors were detected.")
    }

}
