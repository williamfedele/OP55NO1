package generation

internal val REGISTERS = listOf("r15","r14","r13","r12","r11","r10","r9","r8","r7","r6","r5", "r4","r3","r2","r1")
internal const val ZERO_REG = "r0" // always assumed to contain 0

// moon words are 32 bit/4 bytes
internal const val WORD_SIZE = 4
// integers are 32 bits,4 bytes. bit 0 is sign bit. stored in 2's complement
internal const val INT_SIZE = WORD_SIZE
// floating point number are stored in single precision ieee 754 format
internal const val FLOAT_SIZE = WORD_SIZE

// Directive instructions
internal const val ENTRY = "entry"
internal const val ALIGN = "align"
internal const val ORG = "org"
internal const val DW = "dw"
internal const val DB = "db"
internal const val RES = "res"

// Data access instructions
internal const val LOAD_WORD = "lw"
internal const val LOAD_BYTE = "lb"
internal const val STORE_WORD = "sw"
internal const val STORE_BYTE = "sb"

// Arithmetic instructions
  // With register operands
internal const val ADD = "add"
internal const val SUB = "sub"
internal const val MUL = "mul"
internal const val DIV = "div"
internal const val MOD = "mod"
internal const val AND = "and"
internal const val OR = "or"
internal const val NOT = "not"
internal const val EQUAL = "ceq"
internal const val NOT_EQUAL = "cne"
internal const val LESS = "clt"
internal const val LESS_EQUAL = "cle"
internal const val GREATER = "cgt"
internal const val GREATER_EQUAL = "cge"
  // With immediate operands
internal const val ADD_I = "addi"
internal const val SUB_I = "subi"
internal const val MUL_I = "muli"
internal const val DIV_I = "divi"
internal const val MOD_I = "modi"
internal const val AND_I = "andi"
internal const val OR_I = "ori"
internal const val EQUAL_I = "ceqi"
internal const val NOT_EQUAL_I = "cnei"
internal const val LESS_I = "clti"
internal const val LESS_EQUAL_I = "clei"
internal const val GREATER_I = "cgti"
internal const val GREATER_EQUAL_I = "cgei"
internal const val SHIFT_LEFT = "sl"
internal const val SHIFT_RIGHT = "sr"

// Input & Output instructions
internal const val GET_CHAR = "getc"
internal const val PUT_CHAR = "putc"

// Control instructions
internal const val BRANCH_IF_ZERO = "bz"
internal const val BRANCH_IF_NONZERO = "bnz"
internal const val JUMP = "j"
internal const val JUMP_REGISTER = "jr"
internal const val JUMP_LINK = "jl"
internal const val JUMP_LINK_REGISTER = "jlr"
internal const val NO_OP = "nop"
internal const val HALT = "hlt"

// util.m
internal const val UTIL_JUMP_REG = "r15" // used for jump link in provided util.m input/output methods
internal const val UTIL_IO_REG = "r1"
