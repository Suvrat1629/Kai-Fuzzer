package kai.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ExecutionResultTest {

    @Test
    fun `classify success`() {
        val input = FuzzInput(sourceCode = "fun main() {}")
        val res = ExecutionResult(input, exitCode = 0, stdout = "", stderr = "", durationMs = 10)
        assertEquals("SUCCESS", res.classify())
    }

    @Test
    fun `classify timeout`() {
        val input = FuzzInput(sourceCode = "")
        val res = ExecutionResult(input, exitCode = -1, stdout = "", stderr = "TIMEOUT", durationMs = 30000)
        assertEquals("TIMEOUT", res.classify())
    }

    @Test
    fun `classify compiler crash`() {
        val input = FuzzInput(sourceCode = "")
        val res = ExecutionResult(input, exitCode = 1, stdout = "", stderr = "Internal error: something broke", durationMs = 100)
        assertEquals("COMPILER_CRASH", res.classify())
    }

    @Test
    fun `classify compilation error`() {
        val input = FuzzInput(sourceCode = "")
        val res = ExecutionResult(input, exitCode = 1, stdout = "", stderr = "error: unresolved reference", durationMs = 50)
        assertEquals("COMPILATION_ERROR", res.classify())
    }
}
