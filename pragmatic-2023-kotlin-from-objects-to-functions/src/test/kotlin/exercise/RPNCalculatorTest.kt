package exercise

import org.example.exercise.RPNCalculator
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class RPNCalculatorTest {

    private val c = RPNCalculator()

    @Test
    fun `calculate RPN`() {
        expectThat(c.calc("4 5 +")).isEqualTo(9.0)
        expectThat(c.calc("6 2 /")).isEqualTo(3.0)
        expectThat(c.calc("5 6 2 1 + / *")).isEqualTo(10.0)
        expectThat(c.calc("2 5 * 4 + 3 2 * 1 + /")).isEqualTo(2.0)
    }
}
