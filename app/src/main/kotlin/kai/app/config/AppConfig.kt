package kai.app.config

/**
 * Small application configuration loader. Reads from environment variables first,
 * then system properties. Keeps `Main.kt` clean and centralizes defaults.
 */
data class AppConfig(
    val runner: String = "jvm",
    val corpusPath: String? = null,
    val runSeconds: Long? = null,
    val workerCount: Int = Runtime.getRuntime().availableProcessors()
)

object AppConfigLoader {
    fun load(): AppConfig {
        val runner = System.getenv("KAI_RUNNER") ?: System.getProperty("kai.runner") ?: "jvm"
        val corpusPath = System.getenv("KAI_CORPUS_PATH") ?: System.getProperty("kai.corpus.path")
        val runSeconds = (System.getenv("KAI_RUN_SECONDS") ?: System.getProperty("kai.run.seconds"))?.toLongOrNull()
        val workerCount = (System.getenv("KAI_WORKERS") ?: System.getProperty("kai.workers") ?: "").toIntOrNull()
            ?: Runtime.getRuntime().availableProcessors()

        return AppConfig(runner = runner, corpusPath = corpusPath, runSeconds = runSeconds, workerCount = workerCount)
    }
}
