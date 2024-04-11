package generation

import parser.Parser
import semantic.SymbolTableCreator
import java.io.File

fun main() {

    val GRAMMAR_DIR = "src/parser/grammar/"
    val INPUT_DIR = "src/generation/input/"
    val OUTPUT_DIR = "src/generation/output/"

    val files = listOf(
        File("bubblesort"),
        File("simplemain"),
        File("print"),
        File("fibonacci"),
        File("dimensions"),
        File("square"),
        File("float"),
        //File("class"),
        //File("polynomial"),
    )
    for (file: File in files) {
        val parser = Parser (
            srcFile = File("$INPUT_DIR$file.src"),
            tableFileLL1 = File("${GRAMMAR_DIR}ll1.csv"),
            firstFollowSetFile = File("${GRAMMAR_DIR}ll1ff.csv"),
            outputDerive = File("$OUTPUT_DIR$file.outderivation"),
            outputSyntaxErrors = File("$OUTPUT_DIR$file.outsyntaxerrors"),
            outputAST = File("$OUTPUT_DIR$file.outast")
        )
        parser.parse()

        val ast = parser.getAST()
        if (ast.getRoot() != null) {
            val symtabCreator = SymbolTableCreator(
                outputSymbolTables = File("$OUTPUT_DIR$file.outsymboltables"),
                outputSemanticErrors = File("$OUTPUT_DIR$file.outsemanticerrors")
            )
            symtabCreator.create(ast.getRoot())
            symtabCreator.dfs()

            val moonGenerator = MoonGenerator(
                global = symtabCreator.global,
                outputMoon = File("$OUTPUT_DIR$file.moon")
            )
            moonGenerator.generate(ast.getRoot())
        }
        else println("AST was null.")

    }
}
