plugins {
    kotlin("jvm")
    // The new, maintained plugin ID from the StackOverflow thread
    id("com.gradleup.shadow") version "8.3.0"
}

dependencies {
    implementation(project(":shared"))
    val kotlinVersion = project.property("kotlin.version").toString()
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
    implementation("org.zeroturnaround:zt-exec:1.12")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>().configureEach {
    mergeServiceFiles()
    archiveClassifier.set("all")

    manifest {
        attributes["Main-Class"] = "kai.app.MainKt"
    }
}