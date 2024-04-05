package generation

internal val REGISTERS = listOf("r15","r14","r13","r12","r11","r10","r9","r8","r7","r6","r5", "r4","r3","r2","r1")

// Data access instructions
internal val LOAD_WORD = "lw"
internal val LOAD_BYTE = "lb"
internal val STORE_WORD = "sw"
internal val STORE_BYTE = "sb"

// Arithmetic instructions
  // With register operands
internal val ADD = "add"
internal val SUB = "sub"
internal val MUL = "mul"
internal val DIV = "div"
internal val MOD = "mod"
internal val AND = "and"
internal val OR = "or"
internal val NOT = "not"
internal val EQUAL = "ceq"
internal val NOT_EQUAL = "cne"
internal val LESS = "clt"
internal val LESS_EQUAL = "cle"
internal val GREATER = "cgt"
internal val GREATER_EQUAL = "cge"
  // With immediate operands
internal val ADD_I = "addi"
internal val SUB_I = "subi"
internal val MUL_I = "muli"
internal val DIV_I = "divi"
internal val MOD_I = "modi"
internal val AND_I = "andi"
internal val OR_I = "ori"
internal val EQUAL_I = "ceqi"
internal val NOT_EQUAL_I = "cnei"
internal val LESS_I = "clti"
internal val LESS_EQUAL_I = "clei"
internal val GREATER_I = "cgti"
internal val GREATER_EQUAL_I = "cgei"
internal val SHIFT_LEFT = "sl"
internal val SHIFT_RIGHT = "sr"

// Input & Output instructions
internal val GET_CHAR = "getc"
internal val PUT_CHAR = "putc"

// Control instructions
internal val ENTRY = "entry"
internal val BRANCH_IF_ZERO = "bz"
internal val BRANCH_IF_NONZERO = "bnz"
internal val JUMP = "j"
internal val JUMP_REGISTER = "jr"
internal val JUMP_LINK = "jl"
internal val JUMP_LINK_REGISTER = "jlr"
internal val NO_OP = "nop"
internal val HALT = "hlt"
