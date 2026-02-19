package kai.platform

import kai.model.FuzzInput
import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinJVMRunnerTest {
    @Test
    fun `runner stats initial`() = kotlinx.coroutines.runBlocking {
        val runner = KotlinJVMRunner()
        runner.initialize()
        val stats = runner.getStats()
        assertEquals(0, stats.totalExecutions)
        runner.shutdown()
    }
}
