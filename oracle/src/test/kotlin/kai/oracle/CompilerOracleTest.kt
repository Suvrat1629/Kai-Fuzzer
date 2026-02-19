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

        val warning = ExecutionResult(FuzzInput("x"), 0, "", "warning: unused variable", 5)
        val verdictWarning = oracle.classify(warning)
        assertEquals("COMPILER_WARNING", verdictWarning)

        val success = ExecutionResult(FuzzInput("x"), 0, "", "", 5)
        val verdictSuccess = oracle.classify(success)
        assertEquals("COMPILER_SUCCESS", verdictSuccess)
    }
}
