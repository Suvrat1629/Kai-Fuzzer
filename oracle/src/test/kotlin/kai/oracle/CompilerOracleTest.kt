package kai.oracle

import kai.model.ExecutionResult
import kai.model.FuzzInput
import kotlin.test.Test
import kotlin.test.assertEquals

class CompilerOracleTest {

    @Test
    fun `classify compiler crash and error`() = kotlinx.coroutines.runBlocking {
        val oracle = CompilerOracle()

    val crash = ExecutionResult(FuzzInput("x"), 1, "", "internal compiler error: fatal", 10)
        val verdictCrash = oracle.classify(crash)
        assertEquals("COMPILER_CRASH", verdictCrash)

        val error = ExecutionResult(FuzzInput("x"), 1, "", "error: unresolved reference", 5)
        val verdictError = oracle.classify(error)
        assertEquals("COMPILER_ERROR", verdictError)
    }
}
