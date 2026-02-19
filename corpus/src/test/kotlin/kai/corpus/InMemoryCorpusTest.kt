package kai.corpus

import kai.model.FuzzInput
import kai.model.ExecutionResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

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
        assertNotNull(selected)
        assertEquals(input.sourceCode, selected.sourceCode)
    }

    @Test
    fun `prune removes oldest`() = kotlinx.coroutines.runBlocking {
        val corpus = InMemoryCorpus()
        repeat(5) { i ->
            val input = FuzzInput(sourceCode = "code $i", createdAt = i.toLong())
            corpus.add(input, ExecutionResult(input, 0, "", "", 1))
        }
        corpus.prune(3)
        assertEquals(3, corpus.size())
        val stats = corpus.getStats()
        // after pruning oldest two (0,1) remain oldest should be createdAt=2
        assertEquals(2L, stats.oldestTimestamp)
    }

    @Test
    fun `getStats on empty`() = kotlinx.coroutines.runBlocking {
        val corpus = InMemoryCorpus()
        val stats = corpus.getStats()
        assertEquals(0, stats.totalInputs)
        assertEquals(0.0, stats.averageGeneration)
    }
}
