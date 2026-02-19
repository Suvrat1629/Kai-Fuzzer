package kai.interfaces.runner

import kai.model.ExecutionResult
import kai.model.FuzzInput

/*
* Platform-specific executor for Kotlin code.
*/
interface Runner {

    /**
     * Execute the given Kotlin code and return detailed results.
     * Implementation must handle timeouts, resource limits, and cleanup.
     */
    suspend fun execute(input: FuzzInput) : ExecutionResult

    /** Target platform identifier (e.g., "jvm", "js", "native", "wasm") */
    val platform : String

    /** Human-readable name for logging and UI */
    val name: String

    /** Whether this runner can collect code coverage data */
    val supportsCoverage: Boolean get() = false

    /** Maximum execution time in seconds */
    val timeoutSeconds: Long get() = 30

    /** Initialize runner resources */
    suspend fun initialize()

    /** Cleanup runner resources */
    suspend fun shutdown()

    /** Get runner statistics (executions, failures, etc.) */
    suspend fun getStats(): RunnerStats
}
