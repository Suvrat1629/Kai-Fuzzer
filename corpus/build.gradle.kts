plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.1.0"
    id("com.gradleup.shadow") version "8.3.0"
}

dependencies {
    implementation(project(":shared"))

    val kotlinVersion = project.findProperty("kotlin.version")?.toString() ?: "2.1.0"

    // Embeddable Compiler
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:${kotlinVersion}")

    // Json serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>().configureEach {
    mergeServiceFiles()
    archiveClassifier.set("all")
}