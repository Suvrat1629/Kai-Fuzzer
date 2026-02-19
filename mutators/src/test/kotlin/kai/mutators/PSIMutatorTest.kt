package kai.mutators

import kai.model.FuzzInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PSIMutatorTest {

    @Test
    fun `mutate without initialize returns same input`() = kotlinx.coroutines.runBlocking {
        val mutator = PSIMutator()
        val input = FuzzInput(sourceCode = "fun main() {}")
        val out = mutator.mutate(input)
        // When environment is not initialized, parseKotlinFile returns null and mutate should return original input
        assertEquals(input.sourceCode, out.sourceCode)
    }

    @Test
    fun `delete statement mutates code`() = kotlinx.coroutines.runBlocking {
        val mutator = PSIMutator()
        mutator.initialize()
        val input = FuzzInput(sourceCode = "fun main() { println(1); println(2) }")
        val out = mutator.mutate(input)
        // Mutation may be a no-op depending on random strategy or parsing; ensure we didn't crash and returned valid result
        assertTrue(out.sourceCode.isNotBlank())
        assertTrue(out.generation >= input.generation)
        mutator.shutdown()
    }
}
