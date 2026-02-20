# Kai Fuzzer

A small Kotlin multiplatform fuzzer framework for Kotlin source code. This repository contains a shared engine, platform-specific runners (JVM), mutators, oracles, and a simple `app` front-end to run fuzz campaigns.

## Quick overview

- `shared/` - shared interfaces and models (Engine, Corpus, Oracle, Runner, ExecutionResult).
- `engine/` - default fuzzing engine implementation (concurrency, worker loop).
- `platforms/jvm/` - JVM runner that compiles and/or executes Kotlin snippets.
- `mutators/` - mutation strategies (PSI-based mutator, etc.).
- `oracle/` - oracle is used to detect crashes and categorise them
- `corpus/` - in-memory corpus implementation with filesystem persistence.
- `app/` - small CLI/app entrypoint that wires components together.

## Build

Repository layout

```
./
├─ app/        # runnable app that wires components and starts the engine
├─ engine/     # optional engine implementation module
├─ shared/     # shared interfaces and models
├─ platforms/
│  └─ jvm/     # JVM-specific runner implementation
├─ mutators/   # mutation strategies (PSI-based mutator)
├─ oracle/     # oracle to detect and categorise crashes
├─ corpus/     # InMemoryCorpus with filesystem persistence
├─ oracle/     # Oracles (CompilerOracle, GenericOracle, CompositeOracle)
├─ build.gradle.kts
└─ settings.gradle.kts
```

Prerequisites

- JDK 21 (set `JAVA_HOME` to a Java 21 JDK)
- Gradle wrapper included; no system Gradle required

Build

```bash
./gradlew build
```

Or build only the app during development:

```bash
./gradlew :app:build -x test
```

Run (maintainer reproduction)

Use this exact command when sharing logs or artifacts with the maintainer (includes `--no-daemon`):

```bash
KAI_CORPUS_PATH=./my_corpus KAI_RUN_SECONDS=30 ./gradlew :app:run -x test --no-daemon
```

Environment variables

- `KAI_CORPUS_PATH` — where corpus/artifacts are saved
- `KAI_RUN_SECONDS` — bounded run duration (seconds)
- `KAI_RUN_WORKERS` — optional worker count override

Corpus layout (on disk)

- `inputs/` — saved inputs as `input_*.kt`
- `hashes.json` — deduplication hashes
- `results.json` — serialized `ExecutionResult` entries
- `crashes/` — per-crash folders with `input.kt`, `result.json`, `meta.json`

Troubleshooting

- No `crashes/` on disk: crashes are persisted when `Corpus.save()` runs (e.g., on graceful shutdown). If the process is killed early, artifacts may not be written.
- Java version errors: ensure `java -version` reports Java 21.

Tests

```bash
./gradlew test
```
