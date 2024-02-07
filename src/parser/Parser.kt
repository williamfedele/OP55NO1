package parser

import lexer.Lexer
import lexer.Token


// Kotlin doesn't natively have a Stack class, so I simulated one with the closest data structure available.
typealias Stack<T> = ArrayDeque<T>
fun <T> Stack<T>.push(e: T) = add(e)
fun <T> Stack<T>.pop() = removeLastOrNull()
fun <T> Stack<T>.top() = last()
fun <T> Stack<T>.inverseRHSManyPush(e: List<T>) = addAll(e.reversed())

const val EPSILON = "&epsilon"
const val FINAL_SYMBOL = "$"

class Parser(private val lexer: Lexer,
             private val terminals: List<String>,
             private val transitionTable: Map<String, Map<String, String>>,
             private val firstSet: Map<String, List<String>>,
             private val followSet: Map<String, List<String>>) {

    private lateinit var a: Token
    private val s = Stack<String>()
    private var error = false

    fun parse(): Boolean {
        s.push(FINAL_SYMBOL)
        s.push("START")
        a = lexer.nextToken()

        while (s.top() != FINAL_SYMBOL) {
            val x = s.top()

            // debug purposes
            if (a.line == 60) {
                val i = 0
            }

            // skip all comments
            while (a.isComment())
                a = lexer.nextToken()

            // top symbol is a terminal
            if (terminals.contains(x)) {
                // next token should match the expected terminal on the stack
                if (x == a.type.repr) {
                    s.pop()
                    a = lexer.nextToken()
                }
                else {
                    // unexpected terminal
                    skipErrors()
                }
            }
            else {
                // non-terminal, check if the token is valid from the current state
                if (transitionTable.containsKey(x) && transitionTable[x]!!.containsKey(a.type.repr)) {
                    // valid, pop stack and add inverse of RHS to stack (ignore epsilons)
                    s.pop()
                    val rhs = transitionTable[x]!![a.type.repr]!!.split(" ").filter {it != EPSILON}
                    // push RHS to the stack in reverse order
                    s.inverseRHSManyPush(rhs)
                }
                else {
                    // unexpected terminal
                    skipErrors()
                }
            }
        }
        return !(s.top()!= FINAL_SYMBOL || error)
    }

    private fun skipErrors() {
        error = true
        println(a.getErrorMessage())

        // keep looking for tokens until one is in the first or follow set of the top of stack
        // we can then pop it and move on after the error
        while (firstSet[s.top()]?.contains(a.type.repr) != true ) {

            a = lexer.nextToken()
            if (a.type.repr == FINAL_SYMBOL || followSet[s.top()]?.contains(a.type.repr) == true ) {
                s.pop()
                return
            }

        }
    }

}

