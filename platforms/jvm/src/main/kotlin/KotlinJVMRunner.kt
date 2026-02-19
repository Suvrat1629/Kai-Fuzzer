package kai.platform

import kai.interfaces.runner.Runner
import kai.interfaces.runner.RunnerStats
import kai.model.ExecutionResult
import kai.model.FuzzInput
import kotlinx.coroutines.*
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.zeroturnaround.exec.ProcessExecutor
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicLong


class KotlinJVMRunner(
    private val useEmbeddedCompiler: Boolean = true,
    private val maxOutputBytes: Int = 10 * 1024 * 1024
) : Runner {

    override val platform: String = "jvm"
    override val name: String = "Kotlin JVM Runner"
    override val supportsCoverage: Boolean = false
    override val timeoutSeconds: Long =  30

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val totalExecutions = AtomicLong(0)
    private val successfulExecutions = AtomicLong(0)
    private val failedExecutions = AtomicLong(0)
    private val totalDuration = AtomicLong(0)
    private val tempDir: Path = Files.createTempDirectory("kai_fuzzer_")
    private val tempFiles = mutableListOf<File>()

    private val cleanupJob: Job = scope.launch {
        while (isActive) {
            delay(60_000)
            cleanupOldTempFiles()
        }
    }

    override suspend fun execute(input: FuzzInput): ExecutionResult {
        totalExecutions.incrementAndGet()
        val startTime = System.currentTimeMillis()

        return try {
            val result = if (useEmbeddedCompiler) executeEmbedded(input) else executeExternal(input)
            val duration = System.currentTimeMillis() - startTime
            totalDuration.addAndGet(duration)

            if (result.isSuccessful) successfulExecutions.incrementAndGet() else failedExecutions.incrementAndGet()
            result.copy(durationMs = duration)
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            failedExecutions.incrementAndGet()
            ExecutionResult(
                input = input,
                exitCode = -1,
                stdout = "",
                stderr = "Runner Exception: ${e.message}\n${e.stackTraceToString().take(2000)}",
                durationMs = duration,
                exception = e.toString()
            )
        }
    }

    private suspend fun executeEmbedded(input: FuzzInput): ExecutionResult = withContext(Dispatchers.IO) {
        val tempFile = createTempKotlinFile(input.sourceCode)
        val stderrStream = StringBuilder()
        val stdoutStream = StringBuilder()

        val collector = object : MessageCollector {
            private var hasErrors = false
            override fun clear() { hasErrors = false }

            // FIXED: Parameter type must be CompilerMessageLocation? for Kotlin 2.1
            override fun report(
                severity: CompilerMessageSeverity,
                message: String,
                location: CompilerMessageSourceLocation?
            ) {
                val formatted = "$severity: $message" + (location?.let { " at ${it.path}:${it.line}" } ?: "")
                if (severity == CompilerMessageSeverity.ERROR || severity == CompilerMessageSeverity.EXCEPTION) {
                    hasErrors = true
                    stderrStream.appendLine(formatted)
                } else {
                    stdoutStream.appendLine(formatted)
                }
            }
            override fun hasErrors(): Boolean = hasErrors
        }

        val compiler = K2JVMCompiler()
        val arguments = K2JVMCompilerArguments().apply {
            freeArgs = listOf(tempFile.absolutePath)
            destination = Files.createTempDirectory("kai_out_").toString()
            noStdlib = true
            suppressWarnings = true
            // apiVersion and languageVersion are often required for K2
            apiVersion = "2.1"
            languageVersion = "2.1"
        }

        // FIXED: Use the overload that accepts MessageCollector and Services
        val exitCode = compiler.exec(
            collector,
            org.jetbrains.kotlin.config.Services.EMPTY,
            arguments
        )

        ExecutionResult(
            input = input,
            exitCode = exitCode.code,
            stdout = stdoutStream.toString().take(maxOutputBytes),
            stderr = stderrStream.toString().take(maxOutputBytes),
            durationMs = 0 // Duration is calculated by the calling 'execute' function
        )
    }

    private suspend fun executeExternal(input: FuzzInput): ExecutionResult = withContext(Dispatchers.IO) {
        val tempFile = createTempKotlinFile(input.sourceCode)
        val errStream = ByteArrayOutputStream()

        try {
            val result = ProcessExecutor()
                .command("kotlinc", tempFile.absolutePath, "-nowarn", "-d", Files.createTempDirectory("kai_ext_").toString())
                .timeout(timeoutSeconds, TimeUnit.SECONDS)
                .redirectError(errStream)
                .readOutput(true)
                .execute()

            ExecutionResult(
                input = input,
                exitCode = result.exitValue,
                stdout = result.outputUTF8().take(maxOutputBytes), // CORRECTED method call
                stderr = errStream.toString().take(maxOutputBytes),
                durationMs = 0
            )
        } catch (e: TimeoutException) {
            ExecutionResult(
                input = input,
                exitCode = -1,
                stdout = "",
                stderr = "TIMEOUT after ${timeoutSeconds}s",
                durationMs = timeoutSeconds * 1000
            )
        }
    }

    private fun createTempKotlinFile(sourceCode: String): File {
        val file = File.createTempFile("kai_", ".kt", tempDir.toFile())
        file.writeText(sourceCode)
        synchronized(tempFiles) { tempFiles.add(file) }
        return file
    }

    private fun cleanupOldTempFiles() {
        synchronized(tempFiles) {
            val iterator = tempFiles.iterator()
            val cutoff = System.currentTimeMillis() - 300_000
            while (iterator.hasNext()) {
                val file = iterator.next()
                if (!file.exists() || file.lastModified() < cutoff) {
                    file.delete()
                    iterator.remove()
                }
            }
        }
    }

    private fun forceCleanup() {
        synchronized(tempFiles) {
            tempFiles.forEach { if (it.exists()) it.delete() }
            tempFiles.clear()
        }
        tempDir.toFile().deleteRecursively()
    }

    override suspend fun initialize() {}
    override suspend fun shutdown() {
        cleanupJob.cancel()
        forceCleanup()
        scope.cancel()
    }

    override suspend fun getStats(): RunnerStats {
        val total = totalExecutions.get()
        return RunnerStats(
            totalExecutions = total,
            successfulExecutions = successfulExecutions.get(),
            failedExecutions = failedExecutions.get(),
            totalDurationMs = totalDuration.get(),
            averageDurationMs = if (total > 0) totalDuration.get().toDouble() / total else 0.0,
            lastError = null
        )
    }
}