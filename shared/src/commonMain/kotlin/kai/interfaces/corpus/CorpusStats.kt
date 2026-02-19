package kai.interfaces.corpus
/**
 * Statistics about corpus composition and evolution.
 */
data class CorpusStats(
    val totalInputs: Long,
    val uniqueCrashes: Int,
    val averageGeneration: Double,
    val coverageGrowth: Double = 0.0,
    val classifications: Map<String, Int> = emptyMap(),
    val oldestTimestamp: Long = 0,
    val newestTimestamp: Long = 0
)