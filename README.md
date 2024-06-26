# Compiler Design Project

The project was named after my favourite Chopin Nocturne. Since the final stage of the compiler is code generation into our imaginary processor called MOON, I thought it fit.

This is an incomplete compiler for a custom language created for a compiler design university course.

Functionality not implemented:
- Floating point arithmetic
- Struct member function calling
- Recursive and nested free function calls

# Phases
## Lexer

This phase extracts individual tokens from the provided source file.

**Atomic Elements**

*Bold = datatype*
|             |                                    |
|-------------|------------------------------------|
| digit       | 0..9                               |
| nonzero     | 1..9                               |
| letter      | a..z | A..Z                        |
| fraction    | .digit* nonzero | .0               |
| alphanum    | letter | digit | _                 |
| **id**      | letter alphanum*                   |
| **integer** | nonzero digit* \| 0                |
| **float**   | integer fraction [e[+\|−] integer] |


 **Operators and Reserved Words** 
|    |   |      |   |      |         |         |          |      |
|----|---|------|---|------|---------|---------|----------|------|
| == | + | \|   | ( | ;    | if      | public  | read     | impl |
| <> | - | &    | ) | ,    | then    | private | write    |      |
|  < | * | !    | { | .    | else    | func    | return   |      |
|  > | / |      | } | :    | integer | var     | self     |      |
| <= | = |      | [ | ->   | float   | struct  | inherits |      |
| >= |   |      | ] |      | void    | while   | let      |      |

**Comments**

Block comments start with /* and end with */

Inline comments start with //

## Parser

This phase checks if the token stream is valid according to the grammar defining the language.

The LL1 grammar for this language can be found in src/grammar.

## AST Generation

This phase constructs an abstract syntax tree for the provided source file. The grammar file was modified to include semantic actions to execute varying actions. An assignment statement for example will take the last two elements off the stack to create an ASSIGNOP, those two elements being a variable with an expression.

## Semantic Analysis

The primary goal of this phase is to create a symbol table representing the individual scopes present in the source file. Using the generated AST, symbol tables are created for classes, free functions, and their members. 

There are also numerous semantic checks performed such as repeated variable declarations in the same scope, undeclared member functions, etc. Semantic type checking was not implemented.

## MOON Translation

The final phase generates code that be executed by the provided MOON processor. This simulator was developed by the late Peter Grogono during his time at Montreal's Concordia University.

The AST is traversed once more, this time to generate moon code as constructs are processed. 

This was implemented as label-based instead of stack-based for simplicity, though there are a few limitations created. The most obvious being the inability to have repeated variable names in the source file.

Compile the moon.c file. Execution of the .moon output file from the compiler must be ran with the util.m library file provided in moon/samples. This library file is used for printing integers to stdout.

Ex: ```./moon bubblesort.moon util.m```
