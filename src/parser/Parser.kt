package parser

import lexer.Lexer
import lexer.TokenType

typealias Stack<T> = ArrayDeque<T>
fun <T> Stack<T>.push(e: T) = addLast(e)
fun <T> Stack<T>.pop() = removeLastOrNull()
fun <T> Stack<T>.top() = last()
fun <T> Stack<T>.inverseRHSManyPush(e: List<T>) = addAll(e.reversed())

class Parser(private val l: Lexer,
             private val t: List<String>,
             private val m: Map<String, Map<String, String>>) {

    fun parse(): Boolean {
        var error = false
        var s = Stack<String>()
        s.push("$")
        s.push("START")
        var a = l.nextToken()

        while (s.top() != "$") {
            val x = s.top()

            if (a.type == TokenType.ID && a.lexeme == "printarray") {
                val i = 0
            }


            while (a.isComment())
                a = l.nextToken()

            // terminals
            if (t.contains(x)) {
                if (x == a.type.repr) {
                    s.pop()
                    a = l.nextToken()
                }
                else {
                    error = true
                }
            }
            else {
                // non-terminal, check if the token is valid from the current state
                if (m.containsKey(x) && m[x]?.containsKey(a.type.repr) == true) {
                    // valid, pop stack and add inverse of RHS to stack
                    s.pop()
                    // get RHS of the transition
                    val rhs = m[x]!![a.type.repr]!!.split(" ").filter {it != "&epsilon"}
                    // push RHS to the stack in reverse order
                    s.inverseRHSManyPush(rhs)
                }
                else {
                    error = true
                }
            }
        }
        return !(s.top()!= "$" || error)
    }

}

