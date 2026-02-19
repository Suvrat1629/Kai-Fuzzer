package kai.interfaces.oracle

enum class Severity {
    INFO, // Just interesting behavior
    WARNING, // Potential issue
    ERROR, // Definite error but not crash
    CRASH, // Actual crash
    CRITICAL // Severe crash (compiler crash, security issue)
}