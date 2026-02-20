# Kai Fuzzer

A small Kotlin multiplatform fuzzer framework for Kotlin source code. This repository contains a shared engine, platform-specific runners (JVM), mutators, oracles, and a simple `app` front-end to run fuzz campaigns.

## Quick overview

- `shared/` — shared interfaces and models (Engine, Corpus, Oracle, Runner, ExecutionResult).
- `engine/` — default fuzzing engine implementation (concurrency, worker loop).
- `platforms/jvm/` — JVM runner that compiles and/or executes Kotlin snippets.
- `mutators/` — mutation strategies (PSI-based mutator, etc.).
- `corpus/` — in-memory corpus implementation with filesystem persistence.
- `app/` — small CLI/app entrypoint that wires components together.

## Build

From the project root (Linux/macOS/Bash):

```bash
# Kai Fuzzer

Kai Fuzzer is a Kotlin Multiplatform research tool for fuzzing Kotlin source code. It provides a small, modular framework composed of:

# Kai Fuzzer

Kai Fuzzer is a Kotlin Multiplatform tool for fuzzing Kotlin source code. It is organized to be small, modular and easy to extend for research and testing purposes.

This document is written for maintainers and contributors; it contains build/run instructions, the repository layout, troubleshooting notes, and guidance for sharing results with a maintainer.

## Repository layout (top-level)

```
./
├─ app/                # runnable app that wires components and starts the engine
├─ engine/             # default Engine implementation (multiplatform)
├─ shared/             # shared interfaces and models (Engine, Corpus, Oracle, Runner, ExecutionResult)
├─ platforms/
│  └─ jvm/             # JVM-specific runner implementation
├─ mutators/           # mutation strategies (PSI-based mutator)
├─ corpus/             # InMemoryCorpus with filesystem persistence
├─ oracle/             # Oracles (CompilerOracle, GenericOracle, CompositeOracle)
├─ build.gradle.kts
└─ settings.gradle.kts
```

## Prerequisites

- JDK 21 installed and available on PATH. `JAVA_HOME` should point to a Java 21 JDK.
- The repository includes a Gradle wrapper; you do not need a system Gradle installation.
- Recommended: Linux or macOS for development and running the JVM runner.

## Build

To build the entire repository (including tests):

```bash
./gradlew build
```

To build only the `app` module (faster during development):

```bash
./gradlew :app:build -x test
```

## Run (maintainer example)

Run a bounded fuzz session for 30 seconds and persist results to `./my_corpus`. This exact command (includes `--no-daemon`) is recommended when you capture logs/outputs for the maintainer:

```bash
KAI_CORPUS_PATH=./my_corpus KAI_RUN_SECONDS=30 ./gradlew :app:run -x test --no-daemon
```

Environment variables / properties used by the app:

- `KAI_CORPUS_PATH` — directory where corpus and artifacts are written.
- `KAI_RUN_SECONDS` — if set, the engine will run for this many seconds then shutdown.
- `KAI_RUN_WORKERS` — optional override for the number of worker coroutines.

Notes:
- The `app` by default uses the JVM runner. To change runner implementations, update `app/RunnerFactory` or provide a new runner implementing the `Runner` interface in `shared`.

## How it works (high level)

1. The `app` initializes components: Corpus, Mutator, Runner, Oracle and creates a `DefaultFuzzingEngine` (or uses the engine module if present).
2. The Engine starts N worker coroutines. Each worker:
   - selects an input from the corpus (or seeds an initial input)
   - applies a mutator to create a candidate
   - passes it to the Runner which compiles/executes the candidate
   - the Oracle analyzes the `ExecutionResult` and returns a verdict
   - interesting inputs are added to the corpus; crash-worthy results are recorded in the corpus metadata
3. On shutdown (or when the app requests a save), `Corpus.save(path)` writes:
   - `inputs/` — saved source files
   - `hashes.json` — deduplication hashes
   - `results.json` — serialized `ExecutionResult` entries
   - `crashes/` — per-crash subfolders (`input.kt`, `result.json`, `meta.json`)

The design separates concerns so new mutators, oracles, or runners can be added without changing the engine core.

## Corpus file format (quick reference)

- `inputs/input_*.kt` — reproducing Kotlin files
- `hashes.json` — JSON array of content hashes used to avoid storing duplicates
- `results.json` — JSON-serialized map of input id → `ExecutionResult` (exitCode, stdout, stderr, duration, timestamp)
- `crashes/crash_N/` — per-crash folder with `input.kt`, `result.json` (full ExecutionResult), and `meta.json` (summary)

## Troubleshooting

- No `crashes/` folder despite console crash messages: the engine prints crashes as found, but per-crash artifacts are only written when `Corpus.save()` is called (for example during graceful shutdown). If the process is killed before save, you will not see persisted crashes. Consider running the process with `--no-daemon` to capture logs and ensure proper shutdown in CI or recordings.
- Build failures due to Java version: make sure `java -version` reports Java 21 and `JAVA_HOME` points to a Java 21 JDK.

## Tests

Run unit tests for all modules:

```bash
./gradlew test
```

Or run tests for a single module (faster):

```bash
./gradlew :corpus:test
```