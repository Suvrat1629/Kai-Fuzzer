package kai.corpus

import kai.interfaces.corpus.Corpus
import kai.interfaces.corpus.CorpusStats
import kai.model.ExecutionResult
import kai.model.FuzzInput
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.random.Random

open class InMemoryCorpus : Corpus {
    protected val inputs = mutableListOf<FuzzInput>()
    protected val results: MutableMap<String, ExecutionResult> = mutableMapOf()
    protected val mutex = Mutex()
    protected val seenHashes = mutableSetOf<String>()  // Use Set for O(1) 'in' checks

    override suspend fun add(input: FuzzInput, result: ExecutionResult): Boolean = mutex.withLock {
        val contentHash = input.contentHash()
        if (contentHash in seenHashes) return@withLock false

        seenHashes.add(contentHash)
        inputs.add(input)
        results[input.id] = result
        true
    }

    override suspend fun selectForMutation(): FuzzInput? = mutex.withLock {
        if (inputs.isEmpty()) return@withLock null

        // Evolutionary strategy: prefer newer and more interesting inputs
        val weights = inputs.mapIndexed { index, input ->
            val ageWeight = (inputs.size - index).toDouble() / inputs.size
            val generationWeight = 1.0 / (input.generation + 1)
            ageWeight * 0.7 + generationWeight * 0.3
        }

        val totalWeight = weights.sum()
        val randomVal = Random.nextDouble(totalWeight)  // Fixed Random usage
        var cumulative = 0.0

        for ((index, weight) in weights.withIndex()) {
            cumulative += weight
            if (randomVal <= cumulative) {
                return@withLock inputs[index]
            }
        }

        inputs.last()
    }

    override suspend fun size(): Long = mutex.withLock { inputs.size.toLong() }

    override suspend fun getStats(): CorpusStats = mutex.withLock {
        if (inputs.isEmpty()) {
            return@withLock CorpusStats(0, 0, 0.0)
        }

        val classifications = mutableMapOf<String, Int>()
        results.values.forEach { result ->
            val classification = result.classify()
            classifications[classification] = classifications.getOrDefault(classification, 0) + 1
        }

        val generations = inputs.map { it.generation }
        val avgGeneration = generations.average()

        CorpusStats(
            totalInputs = inputs.size.toLong(),
            uniqueCrashes = classifications.getOrDefault("COMPILER_CRASH", 0),
            averageGeneration = avgGeneration,
            classifications = classifications,
            oldestTimestamp = inputs.minOfOrNull { it.createdAt } ?: 0,
            newestTimestamp = inputs.maxOfOrNull { it.createdAt } ?: 0
        )
    }

    override suspend fun save(path: String) {
        val dir = File(path).apply { mkdirs() }
        inputs.forEachIndexed { index, inp ->
            File(dir, "input_$index.kt").writeText(inp.sourceCode)
        }
        File(dir, "hashes.json").writeText(Json.encodeToString(seenHashes))
        File(dir, "results.json").writeText(Json.encodeToString(results))
    }

    override suspend fun load(path: String) {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return

        dir.listFiles { f -> f.extension == "kt" }?.forEach { file ->
            val source = file.readText()
            val dummyResult = ExecutionResult(
                input = FuzzInput(sourceCode = "dummy"),  // Named param for safety
                exitCode = 0,
                stdout = "",
                stderr = "",
                durationMs = 0
            )
            add(FuzzInput(sourceCode = source), dummyResult)  // Named param
        }
        val hashesFile = File(dir, "hashes.json")
        if (hashesFile.exists()) seenHashes.addAll(Json.decodeFromString(hashesFile.readText()))
        val resultsFile = File(dir, "results.json")
        if (resultsFile.exists()) results.putAll(Json.decodeFromString(resultsFile.readText()))
    }

    override suspend fun prune(maxSize: Int) = mutex.withLock {
        if (inputs.size > maxSize) {
            // Remove oldest inputs first
            inputs.sortBy { it.createdAt }
            val toRemove = inputs.subList(0, inputs.size - maxSize)
            toRemove.forEach { input ->
                results.remove(input.id)
                seenHashes.remove(input.contentHash())
            }
            inputs.subList(0, inputs.size - maxSize).clear()
        }
    }

    override suspend fun findByClassification(classification: String): List<FuzzInput> = mutex.withLock {
        inputs.filter { input ->
            results[input.id]?.classify() == classification
        }
    }
}