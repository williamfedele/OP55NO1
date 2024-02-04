package parser

import lexer.Lexer
import java.io.BufferedReader
import java.io.File

fun main() {
    val mapping = readTable(File("src/parser/grammar/ll1.csv"))


    val reader = BufferedReader(File("src/parser/input/bubblesort.src").reader())
    val lex = Lexer(reader)

    val parser = Parser(lex, mapping.headers, mapping.map)
    println(parser.parse())
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

/*
fun readCSV(file: File): Map<String, List<String>> {
    //val reader = BufferedReader(file.reader())
    val map = mutableMapOf<String, MutableMap<String, String>>()
    file.bufferedReader().forEachLine {
        val line = it.split(",")
        map[line[0]] = line.slice(1..<line.size)
    }
    return map
}*/