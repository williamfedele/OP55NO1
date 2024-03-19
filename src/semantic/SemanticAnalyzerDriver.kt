package semantic

import parser.Parser
import java.io.File

fun main() {
    val GRAMMAR_DIR = "src/parser/grammar/"
    val INPUT_DIR = "src/semantic/input/"
    val OUTPUT_DIR = "src/semantic/output/"

    val files = listOf(
        File("bubblesort.src"),
        //File("polynomial.src"),
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
        val symTabCreationVisitor = SymTabCreationVisitor()
        if (ast.getRoot()!= null) {
            symTabCreationVisitor.visit(ast.getRoot()!!)
            recurPrintHelper(ast.getRoot()!!.symTab!!)
        }



        else println("AST was null.")
    }

}

fun recurPrintHelper(rootSymTab: SymTab) {
    println("|    $rootSymTab")
    println("|    =============================================================")
    recurPrint(rootSymTab)
}
fun recurPrint(rootSymTab: SymTab, padding: String = "|") {
    for (entry : SymTabEntry in rootSymTab.entries) {
        println("$padding    $entry")
        if (entry.type == EntryType.CLASS)
            println("$padding    =============================================================")
        if (entry.innerSymTab != null) {
            println("$padding        ${entry.innerSymTab}")
            println("$padding        =============================================================")
            recurPrint(entry.innerSymTab, "$padding    ")
        }

    }
}