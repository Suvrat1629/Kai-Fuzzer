import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm") version "2.1.0" apply false
    kotlin("multiplatform") version "2.1.0" apply false
    id("org.jetbrains.dokka") version "1.9.20" apply false
}

allprojects {
    repositories {
        mavenCentral()
        google()
    }

    // Use withType<KotlinJvmCompile> to avoid breaking KMP targets like JS/Native
    tasks.withType<KotlinJvmCompile>().configureEach {
        kotlinOptions {
            // Align Kotlin JVM target with the JDK used by the build (JDK 21 in CI/dev machines)
            jvmTarget = "21"
            apiVersion = "2.1"
            languageVersion = "2.1"
            freeCompilerArgs = listOf("-Xjsr305=strict", "-Xjvm-default=all")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        // Good practice for your Arch machine's performance
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
    }
}

// Fixed the deprecated buildDir references
tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
    subprojects.forEach {
        delete(it.layout.buildDirectory)
    }
}