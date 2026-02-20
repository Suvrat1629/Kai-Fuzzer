plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":corpus"))
    implementation(project(":mutators"))
    implementation(project(":oracle"))
    implementation(project(":platforms:jvm"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${property("coroutines.version")}")
}

application {
    // Kotlin top-level `main` becomes `MainKt` in the package
    mainClass.set("kai.app.MainKt")
}
