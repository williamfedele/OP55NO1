package parser

import lexer.Lexer
import java.io.BufferedReader
import java.io.File

const val GRAMMAR_DIR = "src/parser/grammar/"
const val INPUT_DIR = "src/parser/input/"

fun main() {
    val mapping = readTable(File("${GRAMMAR_DIR}ll1.csv"))

    val files = listOf(
        File("${INPUT_DIR}bubblesort.src"),
        File("${INPUT_DIR}polynomial.src")
    )

    val first = readFirstFollow(File("${GRAMMAR_DIR}ll1first.csv"))
    val follow = readFirstFollow(File("${GRAMMAR_DIR}ll1follow.csv"))

    val lex = Lexer(files[1])

    // TODO: the parser will always need a lexer. should the parser handle its creation or the driver?
    // if the parser internally creates a lexer, would just need to pass the file.
    // could also update the file and have the parser update the lexer internally without creating a new object
    val parser = Parser(lexer = lex,
        terminals = mapping.headers,
        transitionTable = mapping.map,
        firstSet = first,
        followSet = follow
    )

    if(parser.parse())
        println("The file is valid.")
    else
        println("Errors were detected.")

}

/**
 * Read a CSV of an 2D LL(1) table.
 * First column corresponds to non-terminals.
 * First row corresponds to terminals.
 * The table is filled with transitions using the terminal from non-terminal state.
 */
fun readTable(file: File) : HeaderMapping {
    val reader = BufferedReader(file.reader())
    val headers = reader.readLine().split(",")
    val map = mutableMapOf<String, Map<String, String>>()
    reader.forEachLine { iter ->
        val line = iter.split(",")
        val first = line[0]
        val rest = line.slice(1..<line.size)
        val subMap: Map<String, String> = headers.zip(rest).toMap()
        // NOTE: remove .filterValues if empty mappings are needed
        map[first] = subMap.filterValues { it.isNotEmpty() }
    }
    return HeaderMapping(headers, map)
}

data class HeaderMapping(val headers: List<String>, val map: Map<String, Map<String, String>>)

/**
 * Read in CSV for first and follow sets.
 * First column corresponds to current terminal/non-terminal.
 * Rest of columns in each line correspond to the set (first or follow).
 */
fun readFirstFollow(file: File): Map<String, List<String>> {
    val reader = BufferedReader(file.reader())
    val map = mutableMapOf<String, List<String>>()
    reader.forEachLine {
        val line = it.split(",")
        map[line[0]] = line.slice(1..<line.size)
    }
    return map
}