package kai.interfaces.corpus

import kai.model.ExecutionResult
import kai.model.FuzzInput
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

/**
 * Manages collection of interesting inputs for evolutionary fuzzing.
 * Provides selection strategies and deduplication.
 */
interface Corpus {
    /** Add an input if it's interesting and unique */
    suspend fun add(input: FuzzInput, result: ExecutionResult): Boolean

    /** Select an input for mutation using evolutionary strategy */
    suspend fun selectForMutation(): FuzzInput?

    /** Current number of inputs in corpus */
    suspend fun size(): Long

    /** Get statistics for monitoring and analysis */
    suspend fun getStats(): CorpusStats

    /** Save corpus to persistent storage */
    suspend fun save(path: String)

    /** Load corpus from persistent storage */
    suspend fun load(path: String)

    /** Remove old or uninteresting inputs (garbage collection) */
    suspend fun prune(maxSize: Int = 1000)

    /** Find inputs by classification */
    suspend fun findByClassification(classification: String): List<FuzzInput>
}