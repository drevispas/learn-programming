package exercise

import org.example.exercise.FunStack
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotSameInstanceAs
import strikt.assertions.isSameInstanceAs

class FunStackTest {

    @Test
    fun `push into the stack`() {
        val stack1 = FunStack<Char>().push('A')
        val stack2 = stack1.push('B')

        println("stack1: $stack1")
        println("stack2: $stack2")

        expectThat(stack1.size()).isEqualTo(1)
        expectThat(stack2.size()).isEqualTo(2)
        expectThat(stack1).isNotSameInstanceAs(stack2)
        expectThat(stack1.stack).isNotSameInstanceAs(stack2.stack)
        expectThat(stack1.stack[0]).isSameInstanceAs(stack2.stack[1])
    }

    @Test
    fun `push push pop`() {
        val (b, stack) = FunStack<Char>().push('A').push('B').pop()
        expectThat(stack.size()).isEqualTo(1)
        expectThat(b).isEqualTo('B')
    }
}
