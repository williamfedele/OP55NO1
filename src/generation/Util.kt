package generation

import ast.Node
import ast.NodeLabel

class Util {

    companion object {
        fun processDimList(dimlist: List<Node>): Int {
            var multDim = 1
            for (str in dimlist) {
                if (str.name == NodeLabel.EMPTY.toString()) {
                    return 0
                }
                try {
                    val parsedInt = str.t!!.lexeme.toInt()
                    multDim *= parsedInt
                } catch (e: NumberFormatException) {
                    // not a valid int
                }
            }
            return multDim
        }
    }
}