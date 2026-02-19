package kai.interfaces.oracle

/**
 * Oracle statistics
 */
data class OracleStats(
    val totalAnalyses: Long,
    val uniqueCrashes: Int,
    val uniqueWarnings: Int,
    val classifications: Map<String, Int>,
    val avgAnalysisTimeMs: Double
)