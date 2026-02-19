package kai.oracle

import kai.interfaces.oracle.Oracle
import kai.interfaces.oracle.OracleStats
import kai.interfaces.oracle.OracleVerdict
import kai.interfaces.oracle.Severity
import kai.model.ExecutionResult

class CompositeOracle(
    private val oracles : List<Oracle>
) : Oracle {
    override suspend fun analyze(result: ExecutionResult): OracleVerdict {
        val verdicts = oracles.map { it.analyze(result) }

        // If no oracles, return a safe default
        if (verdicts.isEmpty()) {
            return OracleVerdict(
                isInteresting = false,
                isCrash = false,
                classification = "NORMAL",
                severity = Severity.INFO
            )
        }

    // Combine the verdicts (Severe is given higher priority )
    val mostSevere = verdicts.maxByOrNull { it.severity.ordinal } ?: verdicts.first()

        // Now Combine the classifications
        val combinedClassification = verdicts
            .map { it.classification }
            .filter { it != "NORMAL" }
            .joinToString ( " | " )
            .ifEmpty { "NORMAL" }
        
        return mostSevere.copy(
            classification = combinedClassification,
            details = mostSevere.details +
                    ("oracleCount" to verdicts.size)
        )
    }

    override suspend fun isCrash(result: ExecutionResult): Boolean {
        return oracles.any { it.isCrash(result)}
    }

    override suspend fun classify(result: ExecutionResult): String {
        return oracles.map { it.classify(result) }
            .filter { it != "NORMAL" }
            .joinToString ( " | " )
            .ifEmpty { "NORMAL" }
    }

    override suspend fun getStats(): OracleStats {
        val allStats = oracles.map { it.getStats() }
        return OracleStats(
            totalAnalyses = allStats.sumOf { it.totalAnalyses },
            uniqueCrashes = allStats.sumOf { it.uniqueCrashes },
            uniqueWarnings = allStats.sumOf { it.uniqueWarnings },
            classifications = allStats.flatMap { it.classifications.entries }
                .groupBy ( { it.key }, {it.value} )
                .mapValues { it.value.sum() },
            avgAnalysisTimeMs = if (allStats.isNotEmpty()) {
                allStats.map { it.avgAnalysisTimeMs }.average()
            } else 0.0
        )
    }

    override suspend fun reset() {
        oracles.forEach { it.reset() }
    }
}