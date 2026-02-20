package kai.app

import kai.interfaces.runner.Runner
import kai.platform.KotlinJVMRunner

/**
 * Simple factory to create a Runner instance based on the configured platform.
 *
 * Extend this to support other runners (e.g., JSRunner) and register them here.
 */
object RunnerFactory {
    fun create(runnerId: String): Runner {
        return when (runnerId.lowercase()) {
            "jvm" -> KotlinJVMRunner()
            "kotlin-jvm" -> KotlinJVMRunner()
            // Will add other implementations here later, example "js" -> JSRunner()
            else -> throw IllegalArgumentException("Unknown runner: $runnerId. Supported: jvm")
        }
    }
}
