package kai.app

import kai.engine.DefaultFuzzingEngine
import kai.corpus.InMemoryCorpus
import kai.mutators.PSIMutator
import kai.oracle.CompilerOracle
import kai.app.config.AppConfigLoader
import kai.app.RunnerFactory
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

fun main() = runBlocking {
    println("🛠️ Initializing Kai Fuzzer for K2...")

    val config = AppConfigLoader.load()
    val runner = RunnerFactory.create(config.runner)
    val mutator = PSIMutator()
    val corpus = InMemoryCorpus()
    val oracle = CompilerOracle()
    val corpusPath = config.corpusPath

    // 1.a Initialize components that require setup (suspend functions)
    try {
        mutator.initialize()
        runner.initialize()
    } catch (e: Exception) {
        println("Failed to initialize components: ${e.message}")
        return@runBlocking
    }

    // 1.b Load corpus if a path is provided
    if (!corpusPath.isNullOrBlank()) {
        try {
            corpus.load(corpusPath)
            println("Loaded corpus from $corpusPath")
        } catch (e: Exception) {
            println("Warning: failed to load corpus from $corpusPath: ${e.message}")
        }
    }

    // 2. Shared orchestration engine
    val engine = DefaultFuzzingEngine(
        corpus = corpus,
        mutator = mutator,
        runner = runner,
        oracle = oracle,
        workerCount = config.workerCount
    )

    // 3. Create a stop signal that the main coroutine waits on. Both the shutdown
    // hook and an optional timer will complete this to end the run cleanly.
    val stopSignal = CompletableDeferred<Unit>()

    // 4. Install graceful shutdown hook that stops engine and shuts down components
    Runtime.getRuntime().addShutdownHook(Thread {
        println("Shutdown signal received — stopping Kai fuzzer...")
        try {
            // Immediate non-blocking stop to avoid hanging shutdown hooks
            engine.stopNow()

            // Attempt graceful suspend-based shutdowns; runBlocking is used but
            // protected with try/catch in case dispatchers are unavailable during JVM shutdown.
            try {
                kotlinx.coroutines.runBlocking {
                    runner.shutdown()
                    mutator.shutdown()
                    if (!corpusPath.isNullOrBlank()) {
                        try {
                            corpus.save(corpusPath)
                            println("Corpus saved to $corpusPath")
                        } catch (e: Exception) {
                            println("Warning: failed to save corpus: ${e.message}")
                        }
                    }
                }
            } catch (t: Throwable) {
                println("Warning: suspend shutdown failed during JVM shutdown: ${t.message}")
            }
        } catch (t: Throwable) {
            println("Error during shutdown: ${t.message}")
        } finally {
            if (!stopSignal.isCompleted) stopSignal.complete(Unit)
        }
    })

    // 4. Launch the fuzzing campaign
    engine.start()

    println("🚀 Fuzzing loop active on ${Runtime.getRuntime().availableProcessors()} cores.")

    // Optional bounded run support via configuration
    val runSeconds = config.runSeconds

    // If runSeconds is set, schedule a coroutine to stop after that duration
    if (runSeconds != null && runSeconds > 0) {
        println("Running for $runSeconds seconds (bounded mode)")
        launch {
            delay(runSeconds * 1000)
            println("Bounded run time reached — initiating shutdown")
            try {
                engine.stopNow()
                try {
                    runner.shutdown()
                    mutator.shutdown()
                    if (!corpusPath.isNullOrBlank()) {
                        try {
                            corpus.save(corpusPath)
                            println("Corpus saved to $corpusPath")
                        } catch (e: Exception) {
                            println("Warning: failed to save corpus: ${e.message}")
                        }
                    }
                } catch (t: Throwable) {
                    println("Warning: graceful shutdown failed in bounded run: ${t.message}")
                }
            } finally {
                if (!stopSignal.isCompleted) stopSignal.complete(Unit)
            }
        }
    }

    // Wait until either the shutdown hook or the bounded run completes the stopSignal
    stopSignal.await()
}
