package kai.interfaces.oracle

import kai.model.ExecutionResult

/**
 * Oracle analyzes execution results to detect bugs, crashes, and interesting behaviors.
 * The interface is platform-agnostic, but implementations may be platform-specific.
 */
interface Oracle {
    /** Analyze execution result and return verdict */
    suspend fun analyze(result: ExecutionResult): OracleVerdict

    /** Check if result indicates a crash/bug */
    suspend fun isCrash(result: ExecutionResult): Boolean

    /** Get detailed classification of the result */
    suspend fun classify(result: ExecutionResult): String

    /** Get oracle statistics */
    suspend fun getStats(): OracleStats

    /** Reset oracle state (for new fuzzing session) */
    suspend fun reset()
}