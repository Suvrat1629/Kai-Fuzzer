package kai.engine

import kai.interfaces.corpus.Corpus
import kai.interfaces.mutator.Mutator
import kai.interfaces.oracle.Oracle
import kai.interfaces.runner.Runner
import kai.model.FuzzInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Minimal default engine implementation included in the `app` module for convenience
 * when a standalone engine module is not available. This implementation is intentionally
 * small and matches the expected interface used by the app.
 */
class DefaultFuzzingEngine(
    private val corpus: Corpus,
    private val mutator: Mutator,
    private val runner: Runner,
    private val oracle: Oracle,
    private val workerCount: Int = 4
) {
    private val running = AtomicBoolean(false)
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Default)

    suspend fun start() {
        if (!running.compareAndSet(false, true)) return

        if (corpus.size() == 0L) {
            seedCorpus()
        }

        repeat(workerCount) { workerId ->
            scope.launch {
                while (true) {
                    if (!running.get()) break

                    try {
                        runIteration(workerId)
                    } catch (c: kotlinx.coroutines.CancellationException) {
                        throw c
                    } catch (t: Throwable) {
                        println("[DefaultFuzzingEngine] worker $workerId iteration error: ${t.message}")
                        delay(50)
                    }
                }
            }
        }
    }

    private suspend fun runIteration(workerId: Int) {
        val parent = corpus.selectForMutation() ?: return
        val mutated = mutator.mutate(parent)
        val result = runner.execute(mutated)
        val verdict = oracle.analyze(result)

        if (verdict.isInteresting) {
            corpus.add(mutated, result)
        }

        if (verdict.isCrash) {
            println("💥 Worker $workerId found a crash: ${verdict.classification}")
        }
    }

    private suspend fun seedCorpus() {
        val initialInput = FuzzInput(sourceCode = "fun main() { println(\"Hello Kai\") }")
        val res = runner.execute(initialInput)
        corpus.add(initialInput, res)
    }

    suspend fun stop() {
        stopNow()
    }

    fun stopNow() {
        running.set(false)
        job.cancelChildren()
        job.cancel()
    }

    fun isRunning(): Boolean = running.get()
}
