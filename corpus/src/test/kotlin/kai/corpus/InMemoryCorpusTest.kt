package kai.corpus

import kai.model.FuzzInput
import kai.model.ExecutionResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InMemoryCorpusTest {

    @Test
    fun `add and dedupe`() = kotlinx.coroutines.runBlocking {
        val corpus = InMemoryCorpus()
        val input = FuzzInput(sourceCode = "fun foo() = 1")
        val result = ExecutionResult(input, 0, "", "", 1)

        val first = corpus.add(input, result)
        assertTrue(first)
        val second = corpus.add(input, result)
        assertFalse(second)
        assertEquals(1, corpus.size())
    }

    @Test
    fun `select for mutation non empty`() = kotlinx.coroutines.runBlocking {
        val corpus = InMemoryCorpus()
        val input = FuzzInput(sourceCode = "fun foo() = 1")
        val result = ExecutionResult(input, 0, "", "", 1)
        corpus.add(input, result)

        val selected = corpus.selectForMutation()
        assertTrue(selected != null)
    }
}
