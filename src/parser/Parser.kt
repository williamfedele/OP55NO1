package parser

import ast.AST
import ast.Node
import ast.Semantic
import lexer.Lexer
import lexer.Token
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter


// Kotlin doesn't natively have a Stack class, so I simulated one with the closest data structure available.
typealias Stack<T> = ArrayDeque<T>
fun <T> Stack<T>.push(e: T) = add(e)
fun <T> Stack<T>.pop() = removeLastOrNull()
fun <T> Stack<T>.top() = last()
fun <T> Stack<T>.inverseRHSManyPush(e: List<T>) = addAll(e.reversed())

const val EPSILON = "&epsilon" // string representation of epsilon found in transition table and first/follow sets
const val FINAL_SYMBOL = "$" // bottom symbol of stack. matches with EOF token reported by lexer

class Parser(srcFile: File,
             tableFileLL1: File,
             firstFollowSetFile: File,
             outputDerive: File,
             outputSyntaxErrors: File,
             outputAST: File? = null) {

    /**
     * The LL1 table's contents are generated by the ucalgary tool using the grammar provided in this project. The first line corresponds to headers/terminals.
     * The first and follow set tables are generated by the ucalgary tool using the grammar provided in this project.
     * Each file was formatted into CSV format.
     */

    private val terminals: List<String> = readTerminals(tableFileLL1)
    private val nonTerminals: List<String> = readNonTerminals(tableFileLL1)
    private val transitionTable: Map<String, Map<String, String>> = readTable(tableFileLL1)
    private val firstFollowSet: Map<String, Map<String, List<String>>> = readFirstFollow(firstFollowSetFile)

    private val lexer: Lexer = Lexer(srcFile)
    private lateinit var currToken: Token
    private lateinit var prevToken: Token
    private val s = Stack<String>()
    private val semanticStack = Stack<Node?>()
    private var error = false

    private val deriveFile = openDerive(outputDerive)
    private val errorFile = openError(outputSyntaxErrors)
    private val astFile = outputAST

    private val derivation = Stack<String>()

    fun parse(): Boolean {
        s.push(FINAL_SYMBOL)
        s.push("START")
        currToken = lexer.nextToken()

        while (s.top() != FINAL_SYMBOL) {
            val x = s.top()

            // debug purposes
            if (x == "A29") {
                val i = 0
            }

            // skip all comments
            while (currToken.isComment())
                currToken = lexer.nextToken()

            // top symbol is a terminal
            if (terminals.contains(x)) {
                // token should match the expected terminal on the stack
                if (x == currToken.type.repr) {
                    writeDerive("TERMINAL")
                    derivation.push(s.pop()!!)
                    prevToken = currToken
                    currToken = lexer.nextToken()
                }
                else {
                    // unexpected terminal
                    skipErrors()
                }
            }
            else if (nonTerminals.contains(x)) {
                // non-terminal, check if the token is valid from the current state
                if (transitionTable.containsKey(x) && transitionTable[x]!!.containsKey(currToken.type.repr)) {
                    val rhs = transitionTable[x]!![currToken.type.repr]!!.split(" ").filter {it != EPSILON}
                    writeDerive("$x->$rhs")
                    // token has valid transition, pop stack and add inverse of RHS to stack (ignore epsilons)
                    s.pop()
                    // push RHS to the stack in reverse order
                    s.inverseRHSManyPush(rhs)
                }
                else {
                    // unexpected terminal
                    skipErrors()
                }
            }
            else {
                processSemantics(x)
                s.pop()
            }
        }
        if (astFile != null) {
            astFile.writeText("") // clear the file
            AST.astPrint(semanticStack.top(), "", astFile)
        }
        return !(s.top()!= FINAL_SYMBOL || error)
    }

    private fun processSemantics(x: String) {
        val action = Semantic.actions[x]

        when (action!!.first) {
            "makeNode" -> semanticStack.push(AST.makeNode(prevToken, action.second))
            "makeNull" -> semanticStack.push(null)
            "makeFamilyUntilNull" -> semanticStack.push(AST.makeFamilyUntilNull(semanticStack, action.second))
            "makeFamily" -> semanticStack.push(AST.makeFamily(semanticStack, action.second, action.third))
        }
    }


    /**
     * Skip erroneous tokens if detected.
     * Scan for tokens until its in the first set of the top of stack non-terminal.
     * Pop if the next token is in the follow set of the top of stack non-terminal.
     */
    private fun skipErrors() {
        error = true
        // print first or follow depending if first is null or not
        writeError("Error with token '${currToken.lexeme}' on line ${currToken.line}. Expected one of the following tokens: ${getPossibleNextTokens()}.")
        writeDerive("ERROR")

        if (currToken.type.repr == FINAL_SYMBOL || firstFollowSet[s.top()]?.get("follow")?.contains(currToken.type.repr) == true) {
            s.pop()
        }
        else {
            // keep looking for tokens until one is in the first or follow set of the top of stack
            // we can then pop it and move on after the error
            while (firstFollowSet[s.top()]?.get("first")?.contains(currToken.type.repr) != true &&
                !((firstFollowSet[s.top()]?.get("first")?.contains(EPSILON) == true && firstFollowSet[s.top()]?.get("follow")?.contains(currToken.type.repr) == true))) {

                currToken = lexer.nextToken()

                if (currToken.type.repr == FINAL_SYMBOL || firstFollowSet[s.top()]?.get("follow")?.contains(currToken.type.repr) == true) {
                    s.pop()
                }
            }
        }


    }

    /**
     * Returns a list containing all possible valid tokens from the current state.
     * For usage in error reporting.
     */
    private fun getPossibleNextTokens(): List<String>? {
        val first = firstFollowSet[s.top()]?.get("first")
        val follow = firstFollowSet[s.top()]?.get("follow")
        // if first and follow are null, the top of the stack is terminal
        if (first == null && follow == null){
            return listOf(s.top())
        }
        else if (first?.contains(EPSILON) == true) { // include follow set if first contains epsilon transition
            return first.filter { it != "&epsilon" } + follow.orEmpty()
        }
        else {
            return first
        }

    }

    /**
     * Terminals are on the first line of the CSV.
     */
    private fun readTerminals(file: File): List<String> {
        val reader = BufferedReader(file.reader())
        return reader.readLine().split(",")
    }

    /**
     * Non-terminals are in the first columns of the CSV.
     */
    private fun readNonTerminals(file: File): List<String> {
        val reader = BufferedReader(file.reader())
        val nonterminals = mutableListOf<String>()
        reader.forEachLine { it ->
            nonterminals.add(it.split(",")[0])
        }
        return nonterminals
    }

    /**
     * Read a CSV of an 2D LL(1) table.
     * First row corresponds to terminals (headers).
     * First column corresponds to non-terminals.
     * The table is filled with transitions using the terminal from non-terminal state.
     */
    private fun readTable(file: File) : Map<String, Map<String, String>> {
        val reader = BufferedReader(file.reader())
        val headers = reader.readLine().split(",")
        val map = mutableMapOf<String, Map<String, String>>()
        reader.forEachLine { iter ->
            val line = iter.split(",")
            val first = line[0] // LHS of production
            val rest = line.slice(1..<line.size) // RHS of production
            // Match the terminals with their RHS from the LHS state
            val subMap: Map<String, String> = headers.zip(rest).toMap()
            // NOTE: remove .filterValues if empty mappings are needed
            map[first] = subMap.filterValues { it.isNotEmpty() }
        }
        return map
    }

    /**
     * Read a CSV for first and follow sets.
     * First column corresponds to the non-terminal.
     * Second column corresponds to the first set of the non-terminal. Space delimited.
     * Third column corresponds to the follow set of the non-terminal. Space delimited.
     */
    private fun readFirstFollow(file: File): Map<String, Map<String, List<String>>> {
        val reader = BufferedReader(file.reader())
        val map = mutableMapOf<String, Map<String, List<String>>>()
        reader.forEachLine {
            val line = it.split(",")
            val innerMap = mutableMapOf<String,List<String>>()
            innerMap["first"] = line[1].split(" ")
            innerMap["follow"] = line[2].split(" ")

            map[line[0]] = innerMap
        }
        return map
    }

    private fun openDerive(file: File): File {
        FileWriter(file).use { out -> out.write("STACK,TOKEN,PRODUCTION\n") }
        return file
    }

    private fun openError(file: File): File {
        FileWriter(file).use { out -> out.write("") }
        return file
    }

    private fun writeError(errorMsg: String) {
        FileWriter(errorFile,true).use { out -> out.write("$errorMsg\n")}
    }


    private fun writeDerive(production: String) {
        FileWriter(deriveFile,true).use { out -> out.write("${derivation.toString().replace(", "," ")}${s.reversed().toString().replace(", "," ")},${currToken},$production\n") }
    }

}

