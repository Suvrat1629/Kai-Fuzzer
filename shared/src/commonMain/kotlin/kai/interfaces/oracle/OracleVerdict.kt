package kai.interfaces.oracle

/**
 * Oracle verdict with detailed analysis
 */
data class OracleVerdict(
    val isInteresting: Boolean, // Should this input be kept?
    val isCrash: Boolean, // Did it crash?
    val classification: String, // Type of behavior
    val severity: Severity, // How severe is the issue?
    val details: Map<String, Any> = emptyMap(), // Additional metadata
    val timestamp: Long = System.currentTimeMillis()
)