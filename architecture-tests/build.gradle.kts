import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.konsist)
}

tasks.test {
    useJUnitPlatform()
    // Konsist scans the sources of the other modules, so they are inputs of this task:
    // without them the build cache can restore a stale result and the guard sees stale code.
    val scannedSources = listOf("server/src/main/kotlin", "api-contract/src") +
        rootProject.file("shared").listFiles().orEmpty()
            .filter { module -> module.isDirectory }
            .map { module -> module.resolve("src") }
    inputs.files(scannedSources.map { path -> rootProject.fileTree(path) })
}
