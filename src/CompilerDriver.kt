import generation.AllocationCalculator
import generation.MoonGenerator
import parser.Parser
import semantic.SymbolTableCreator
import java.io.File

fun main(args: Array<String>) {

    if (args.isEmpty()) {
        println("No input file provided")
        return
    }
    if (!args[0].endsWith(".src")) {
        println("Invalid file type.")
        return
    }
    val file = File(args[0].split(".")[0])

    /**
     * All files: bubblesort, objects, simplemain, print, fibonacci, dimensions, square, float
     */

    val GRAMMAR_DIR = "src/grammar/"
    val INPUT_DIR = "src/input/"
    val OUTPUT_DIR = "src/"

    val parser = Parser (
        srcFile = File("$INPUT_DIR$file.src"),
        tableFileLL1 = File("${GRAMMAR_DIR}ll1.csv"),
        firstFollowSetFile = File("${GRAMMAR_DIR}ll1ff.csv"),
        outputDerive = File("$OUTPUT_DIR$file.outderivation"),
        outputSyntaxErrors = File("$OUTPUT_DIR$file.outsyntaxerrors"),
        outputAST = File("$OUTPUT_DIR$file.outast")
    )
    println("Running parser...")
    parser.parse()
    println("Done!")
    val ast = parser.getAST()
    if (ast.getRoot() != null) {
        val symtabCreator = SymbolTableCreator(
            outputSymbolTables = File("$OUTPUT_DIR$file.outsymboltables"),
            outputSemanticErrors = File("$OUTPUT_DIR$file.outsemanticerrors")
        )
        
        println("Running symbol table creation...")
        symtabCreator.create(ast.getRoot())
        symtabCreator.dfs()
        println("Done!")

        println("Appending object memory allocation size to the symbol table...")
        val allocationCalculator = AllocationCalculator(
            global = symtabCreator.global
        )
        allocationCalculator.traverse(ast.getRoot())
        println("Done!")

        println("Generating moon code...")
        val moonGenerator = MoonGenerator(
            global = symtabCreator.global,
            outputMoon = File("$OUTPUT_DIR$file.moon")
        )
        moonGenerator.generate(ast.getRoot())
        println("Done!")
    }
    else println("AST was null.")
}
