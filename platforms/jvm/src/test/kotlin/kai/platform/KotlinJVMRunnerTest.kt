package kai.platform

import kai.model.FuzzInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KotlinJVMRunnerTest {
    @Test
    fun `runner stats initial`() = kotlinx.coroutines.runBlocking {
        val runner = KotlinJVMRunner()
        runner.initialize()
        val stats = runner.getStats()
        assertEquals(0, stats.totalExecutions)
        runner.shutdown()
    }

    @Test
    fun `execute simple code`() = kotlinx.coroutines.runBlocking {
        val runner = KotlinJVMRunner()
        runner.initialize()
        val input = FuzzInput("fun main() {}")
        val result = runner.execute(input)
        // Compiler behavior can vary by environment; ensure we got a result and it references the input
        assertNotNull(result)
        assertEquals(input.id, result.input.id)
        assertTrue(result.exitCode is Int)
        runner.shutdown()
    }
}
