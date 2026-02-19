package kai.interfaces.runner

/**
 * Statistics about runner performance and reliability.
 */
data class RunnerStats(
    val totalExecutions: Long = 0,
    val successfulExecutions: Long = 0,
    val failedExecutions: Long = 0,
    val totalDurationMs: Long = 0,
    val averageDurationMs: Double = 0.0,
    val lastError: String? = null
)