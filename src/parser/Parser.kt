package parser

import lexer.Lexer
import lexer.Token


// Kotlin doesn't natively have a Stack class, so I simulated one with the closest data structure available.
typealias Stack<T> = ArrayDeque<T>
fun <T> Stack<T>.push(e: T) = add(e)
fun <T> Stack<T>.pop() = removeLastOrNull()
fun <T> Stack<T>.top() = last()
fun <T> Stack<T>.inverseRHSManyPush(e: List<T>) = addAll(e.reversed())

class Parser(private val lexer: Lexer,
             private val terminals: List<String>,
             private val transitionTable: Map<String, Map<String, String>>,
             private val firstSet: Map<String, List<String>>,
             private val followSet: Map<String, List<String>>) {

    private val EPSILON = "&epsilon"
    private val FINAL_SYMBOL = "$"
    private lateinit var a: Token
    private val s = Stack<String>()
    fun parse(): Boolean {
        var error = false
        s.push(FINAL_SYMBOL)
        s.push("START")
        a = lexer.nextToken()

        while (s.top() != FINAL_SYMBOL) {
            val x = s.top()

            // debug purposes
            if (a.line == 60) {
                val i = 0
            }

            while (a.isComment())
                a = lexer.nextToken()

            // terminals
            if (terminals.contains(x)) {
                if (x == a.type.repr) {
                    s.pop()
                    a = lexer.nextToken()
                }
                else {
                    error = true
                    println("Syntax error at line $a.line.")
                    skipErrors()
                }
            }
            else {
                // non-terminal, check if the token is valid from the current state
                if (transitionTable.containsKey(x) && transitionTable[x]!!.containsKey(a.type.repr)) {
                    // valid, pop stack and add inverse of RHS to stack
                    s.pop()
                    // get RHS of the transition (not epsilons)
                    val rhs = transitionTable[x]!![a.type.repr]!!.split(" ").filter {it != EPSILON}
                    // push RHS to the stack in reverse order
                    s.inverseRHSManyPush(rhs)
                }
                else {
                    error = true
                    println("Syntax error at line $a.line.")
                    skipErrors()

                }
            }
        }
        return !(s.top()!= FINAL_SYMBOL || error)
    }

    fun skipErrors() {
        if (a.type.repr == FINAL_SYMBOL || followSet[s.top()]?.contains(a.type.repr) == true )
            s.pop()
        else {
            while (firstSet[s.top()]?.contains(a.type.repr) != true ||
                (firstSet[s.top()]?.contains(EPSILON) == true && followSet[s.top()]?.contains(a.type.repr) != true )) {

                a = lexer.nextToken()
            }
        }
    }

}

