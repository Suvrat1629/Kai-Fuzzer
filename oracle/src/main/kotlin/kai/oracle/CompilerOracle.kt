package kai.oracle

import kai.interfaces.oracle.Oracle
import kai.interfaces.oracle.OracleStats
import kai.interfaces.oracle.OracleVerdict
import kai.interfaces.oracle.Severity
import kai.model.ExecutionResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CompilerOracle : Oracle {

    private val compilerCrashPatterns = listOf(
        "compiler crash",
        "internal compiler error",
        "ice:",
        "panic:",
        "segmentation fault",
        "assertion failed",
        "kotlin: internal error"
    ).map { it.lowercase() }

    private var totalAnalyses = 0L
    private var totalAnalysisTimeMs = 0L
    private var uniqueCrashes = 0
    private val classifications = mutableMapOf<String, Int>()
    private val mutex = Mutex()

    override suspend fun analyze(result: ExecutionResult): OracleVerdict {
    val start = System.currentTimeMillis()
    val classification = classify(result)
    val isCrash = isCrash(result)

        val severity = when {
            classification == "COMPILER_CRASH" -> Severity.CRITICAL
            classification == "COMPILER_ERROR" -> Severity.WARNING
            isCrash -> Severity.CRASH
            else -> Severity.INFO
        }

        val isInteresting = isCrash || classification.startsWith("COMPILER_")

        val analysisTime = System.currentTimeMillis() - start
        mutex.withLock {
            totalAnalyses++
            totalAnalysisTimeMs += analysisTime
            classifications[classification] = classifications.getOrDefault(classification, 0) + 1
            if (isCrash) uniqueCrashes++
        }

        return OracleVerdict(
            isInteresting = isCrash || classification.startsWith("COMPILER_"),
            isCrash = isCrash,
            classification = classification,
            severity = severity,
            details = mapOf(
                "exitCode" to result.exitCode,
                "errorMessage" to extractErrorMessage(result.stderr)
            )
        )
    }

    override suspend fun isCrash(result: ExecutionResult): Boolean {
        val stderr = result.stderr.lowercase()
        return compilerCrashPatterns.any { pattern -> stderr.contains(pattern) } || result.exitCode != 0
    }

    override suspend fun classify(result: ExecutionResult): String {
        val stderr = result.stderr.lowercase()
        
        return when {
            compilerCrashPatterns.any { it in stderr } -> "COMPILER_CRASH"
            stderr.contains("error") -> "COMPILER_ERROR"
            stderr.contains("warning") -> "COMPILER_WARNING"
            result.exitCode != 0 -> "COMPILER_EXIT_${result.exitCode}"
            else -> "COMPILER_SUCCESS"
        }
    }

    override suspend fun getStats(): OracleStats {
        return OracleStats(
            totalAnalyses = totalAnalyses,
            uniqueCrashes=uniqueCrashes,
            uniqueWarnings = classifications.getOrDefault("COMPILER_WARNING", 0),
            classifications= classifications,
            avgAnalysisTimeMs = if (totalAnalyses > 0) totalAnalysisTimeMs.toDouble() / totalAnalyses else 0.0)
    }

    override suspend fun reset() {
        totalAnalyses = 0
        uniqueCrashes = 0
        classifications.clear()
    }

    private fun extractErrorMessage(stderr: String): String {
        return stderr.lines()
            .firstOrNull { it.lowercase().contains("error") || it.lowercase().contains("panic") }
            ?.take(200) ?: ""
    }

}