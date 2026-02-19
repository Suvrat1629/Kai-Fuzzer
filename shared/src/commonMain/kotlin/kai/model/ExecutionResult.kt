package kai.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.datetime.Instant


/**
 * Result of compiling and optionally executing a Kotlin program.
 * @property input The original fuzz input
 * @property exitCode Process exit code (0 for success, non-zero for errors)
 * @property stdout Standard output from compilation
 * @property stderr Standard error output (contains compiler messages)
 * @property durationMs Execution time in milliseconds
 * @property coverageData Optional coverage information (JSON format)
 * @property exception Optional exception if runner failed
 * @property timestamp When this result was recorded
 */
@Serializable
data class ExecutionResult(
    val input: FuzzInput,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMs: Long,
    val coverageData: String?=null,
    val exception: String?=null,
    val timestamp: Long = System.currentTimeMillis()
) {
     companion object {
        /**
         * Parse from JSON string.
         */
        public fun fromJson(json: String): ExecutionResult {
            return Json.decodeFromString(serializer(), json)
        }
    }

    /**
     * Check if compilation was successful (exit code 0).
     */
    val isSuccessful: Boolean get() = exitCode == 0

    /**
     * Check if compilation failed due to user code error.
     * Different from compiler crash - this is expected behavior.
     */
    val isCompilationError: Boolean get() = !isSuccessful
            && stderr.contains("error:")
            && stderr.contains("Exception")

    /**
     * Check if compiler crashed (internal error, assertion, etc.)
     */
    val isCompilerCrash : Boolean get() = !isSuccessful &&
            (stderr.contains("Internal error")||
                stderr.contains("java.lang.") ||
                stderr.contains("kotlin.") ||
                stderr.contains("AssertionError"))

    /**
     * Check if execution timed out.
     */
    val isTimeout: Boolean get() = exitCode == -1 && stderr.contains("TIMEOUT")

    /**
     * Check if process was killed (OOM, signal, etc.)
     */
    val isKilled: Boolean get() = exitCode in 128..255  // Signal codes on Unix

    /**
     * Get a summary classification of the result.
     */
    fun classify(): String = when {
        isSuccessful -> "SUCCESS"
        isTimeout -> "TIMEOUT"
        isKilled -> "KILLED"
        isCompilerCrash -> "COMPILER_CRASH"
        isCompilationError -> "COMPILATION_ERROR"
        else -> "UNKNOWN_FAILURE"
    }

    /**
     * Convert to JSON string for persistence.
     */
    fun toJson(): String = Json.encodeToString(serializer(), this)
}