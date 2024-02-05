package parser

import lexer.Lexer
import java.io.BufferedReader
import java.io.File

fun main() {
    val mapping = readTable(File("src/parser/grammar/ll1.csv"))


    val reader = BufferedReader(File("src/parser/input/bubblesort.src").reader())
    //val reader = BufferedReader(File("src/parser/input/polynomial.src").reader())

    val first = readFirstFollow(File("src/parser/grammar/ll1first.csv"))
    val follow = readFirstFollow(File("src/parser/grammar/ll1follow.csv"))


    val lex = Lexer(reader)

    val parser = Parser(lexer = lex,
        terminals = mapping.headers,
        transitionTable = mapping.map,
        firstSet = first,
        followSet = follow)

    if(parser.parse())
        println("The file is valid.")
    else
        println("The file is not valid.")


    val i = 0
}

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


fun readFirstFollow(file: File): Map<String, List<String>> {
    val reader = BufferedReader(file.reader())
    val map = mutableMapOf<String, List<String>>()
    reader.forEachLine {
        val line = it.split(",")
        map[line[0]] = line.slice(1..<line.size)
    }
    return map
}