package ast

import parser.Parser
import java.io.File

const val GRAMMAR_DIR = "src/parser/grammar/"
const val INPUT_DIR = "src/ast/input/"
const val OUTPUT_DIR = "src/ast/output/"

fun main() {

    val files = listOf(
        File("example1.src")
    )

    for (file: File in files) {
        val parser = Parser (
            srcFile = File("$INPUT_DIR$file"),
            tableFileLL1 = File("${GRAMMAR_DIR}ll1.csv"),
            firstFollowSetFile = File("${GRAMMAR_DIR}ll1ff.csv"),
            outputDerive = File("$OUTPUT_DIR$file.outderivation"),
            outputSyntaxErrors = File("$OUTPUT_DIR$file.outsyntaxerrors"),
            outputAST = File("$OUTPUT_DIR$file.outast")
        )
        parser.parse()
    }
}