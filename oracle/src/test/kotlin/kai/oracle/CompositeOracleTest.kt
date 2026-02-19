package kai.oracle

import kai.interfaces.oracle.Oracle
import kai.interfaces.oracle.OracleVerdict
import kai.interfaces.oracle.Severity
import kai.model.ExecutionResult
import kai.model.FuzzInput
import kotlin.test.Test
import kotlin.test.assertEquals

class CompositeOracleTest {

    @Test
    fun `combine verdicts and pick most severe`() = kotlinx.coroutines.runBlocking {
        val o1 = object : Oracle {
            override suspend fun analyze(result: ExecutionResult) = OracleVerdict(true, false, "A", Severity.INFO)
            override suspend fun isCrash(result: ExecutionResult) = false
            override suspend fun classify(result: ExecutionResult) = "A"
            override suspend fun getStats() = throw NotImplementedError()
            override suspend fun reset() {}
        }

        val o2 = object : Oracle {
            override suspend fun analyze(result: ExecutionResult) = OracleVerdict(true, false, "B", Severity.CRITICAL)
            override suspend fun isCrash(result: ExecutionResult) = false
            override suspend fun classify(result: ExecutionResult) = "B"
            override suspend fun getStats() = throw NotImplementedError()
            override suspend fun reset() {}
        }

        val composite = CompositeOracle(listOf(o1, o2))
        val res = ExecutionResult(FuzzInput(""), 0, "", "", 1)
        val verdict = composite.analyze(res)
        // combinedClassification preserves the order of oracles: "A | B"
        assertEquals("A | B", verdict.classification)
        assertEquals(Severity.CRITICAL, verdict.severity)
    }

    @Test
    fun `empty oracles fallback`() = kotlinx.coroutines.runBlocking {
        val composite = CompositeOracle(emptyList())
        val res = ExecutionResult(FuzzInput(""), 0, "", "", 1)
        val verdict = composite.analyze(res)
        assertEquals("NORMAL", verdict.classification)
        assertEquals(Severity.INFO, verdict.severity)
    }
}
