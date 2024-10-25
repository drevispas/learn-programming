package org.example.exercise

class RPNCalculator {

    val operationMap: Map<String, (Double, Double) -> Double> = mapOf(
        "+" to Double::plus,
        "-" to Double::minus,
        "*" to Double::times,
        "/" to Double::div
    )

    val funStack: FunStack<Double> = FunStack()

    // "5 6 2 1 + / * "
    fun calc(expr: String): Double = expr.split(" ").fold(funStack, ::reduce).pop().first

    private fun reduce(stack: FunStack<Double>, token: String): FunStack<Double> =
        if (operationMap.containsKey(token)) {
            val (opr1, tempStack) = stack.pop()
            val (opr2, newStack) = tempStack.pop()
            newStack.push(operate(token, opr2, opr1))
        } else {
            stack.push(token.toDouble())
        }

    private fun operate(token: String, a: Double, b: Double): Double =
        operationMap[token]?.invoke(a, b) ?: error("Invalid operator `$token`!")
}
