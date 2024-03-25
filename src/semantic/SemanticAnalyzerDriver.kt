package semantic

import parser.Parser
import java.io.File

fun main() {
    val GRAMMAR_DIR = "src/parser/grammar/"
    val INPUT_DIR = "src/semantic/input/"
    val OUTPUT_DIR = "src/semantic/output/"

    val files = listOf(
        File("bubblesort.src"),
        File("polynomial.src"),
        File("polynomialsemanticerrors.src")
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

        val ast = parser.getAST()
        if (ast.getRoot()!= null) {
            val symtabCreator = SymbolTableCreator(
                outputSymbolTables = File("$OUTPUT_DIR$file.outsymboltables"),
                outputSemanticErrors = File("$OUTPUT_DIR$file.outsemanticerrors")
            )
            symtabCreator.create(ast.getRoot()!!)
            symtabCreator.dfs()
        }
        else println("AST was null.")

    }
}
