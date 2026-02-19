plugins {
    kotlin("jvm")
    id("com.gradleup.shadow") version "8.3.0"
}

dependencies {
    implementation(project(":shared"))

    // Get version from gradle.properties or extra
    val kotlinVersion = project.findProperty("kotlin.version")?.toString() ?: "2.1.0"

    // Kotlin compiler and reflection
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    implementation("org.zeroturnaround:zt-exec:1.12")

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