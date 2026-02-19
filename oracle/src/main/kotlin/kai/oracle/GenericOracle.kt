package kai.oracle

import kai.interfaces.oracle.Oracle
import kai.interfaces.oracle.OracleStats
import kai.interfaces.oracle.OracleVerdict
import kai.interfaces.oracle.Severity
import kai.model.ExecutionResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GenericOracle : Oracle {

    private var totalAnalyses = 0L
    private var totalAnalysisTimeMs = 0L
    private val crashSignatures = mutableMapOf<String, Int>()
    private val mutex = Mutex()

    private val crashPatterns = mapOf(
        -11 to "SEGMENTATION_FAULT", // SIGSEGV
        -6 to "ABORT", // SIGABRT
        -8 to "FPE", // Floating point exception
        -4 to "ILLEGAL_INSTRUCTION", // SIGILL
        134 to "ABORT", // 128 + SIGABRT
        139 to "SEGMENTATION_FAULT", // 128 + SIGSEGV
        136 to "FPE" // 128 + SIGFPE
    )

    override suspend fun analyze(result: ExecutionResult): OracleVerdict {
        val start = System.currentTimeMillis()

        val classification = classify(result)
        val isCrash = isCrash(result)
        val severity = when {
            isCrash && classification.contains("COMPILER") -> Severity.CRITICAL
            result.exitCode != 0 -> Severity.ERROR
            result.durationMs > 5000 -> Severity.WARNING
            else -> Severity.INFO
        }

        val isInteresting = isCrash ||
                            result.exitCode != 0 ||
                            result.durationMs > 5000 ||
                            hasInterestingOutput(result)

        val end = System.currentTimeMillis()
        val analysisTime = end - start

        mutex.withLock {
            totalAnalyses++
            totalAnalysisTimeMs += analysisTime
            if (isCrash) {
                crashSignatures[classification] = crashSignatures.getOrDefault(classification,0) + 1
            }
        }

        return OracleVerdict(
            isInteresting = isInteresting,
            isCrash = isCrash,
            classification = classification,
            severity = severity,
            details = mapOf(
                "exitCode" to result.exitCode,
                "durationMs" to result.durationMs,
                "outputSize" to (result.stdout.length + result.stderr.length),
                "signal" to (result.exitCode < 0)
            )
        )
    }

    override suspend fun isCrash(result: ExecutionResult): Boolean {

        if(result.exitCode in crashPatterns) return true

        val stderr = result.stderr.lowercase()
        return stderr.contains("segmentation fault") ||
                stderr.contains("segfault") ||
                stderr.contains("abort") ||
                stderr.contains("illegal instruction") ||
                stderr.contains("floating point exception")
    }

    override suspend fun classify(result: ExecutionResult): String {
        return when {
            result.exitCode in crashPatterns -> crashPatterns[result.exitCode]!!
            result.durationMs > 5000 -> "TIMEOUT"
            result.exitCode != 0 -> "EXIT_${result.exitCode}"
            else -> "NORMAL"
        }
    }

    override suspend fun getStats(): OracleStats = mutex.withLock {
        OracleStats(
            totalAnalyses = totalAnalyses,
            uniqueCrashes = crashSignatures.size,
            uniqueWarnings = 0,
            classifications = crashSignatures,
            avgAnalysisTimeMs = if (totalAnalyses > 0) totalAnalysisTimeMs.toDouble() / totalAnalyses else 0.0
        )
    }

    override suspend fun reset() {

        mutex.withLock {
            totalAnalyses = 0
            totalAnalysisTimeMs = 0
            crashSignatures.clear()
        }
    }

    private fun hasInterestingOutput(result: ExecutionResult) : Boolean{
        val patterns = listOf(
            "error", "warning", "exception", "panic",
            "invalid", "unexpected", "failed"
        )

        val output = (result.stdout + result.stderr).lowercase()
        return patterns.any { it in output }
    }
}