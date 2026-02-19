package kai.mutators

import kai.model.FuzzInput
import kotlin.test.Test
import kotlin.test.assertEquals

class PSIMutatorTest {

    @Test
    fun `mutate without initialize returns same input`() = kotlinx.coroutines.runBlocking {
        val mutator = PSIMutator()
        val input = FuzzInput(sourceCode = "fun main() {}")
        val out = mutator.mutate(input)
        // When environment is not initialized, parseKotlinFile returns null and mutate should return original input
        assertEquals(input.sourceCode, out.sourceCode)
    }
}
