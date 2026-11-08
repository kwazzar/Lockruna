import dev.detekt.gradle.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask

plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidKmpLibrary) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.skie) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "dev.detekt")

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig.set(true)
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        source.setFrom(
            fileTree(projectDir) {
                include("src/**/*.kt")
                exclude("**/build/**")
            },
        )
    }
}

gradle.projectsEvaluated {
    subprojects {
        // Only hand-written sources are linted: SQLDelight and the Kotlin compiler
        // write generated Kotlin into build/, which is not ours to format.
        // The tasks are realized first so that the plugin has already set its own source.
        tasks.withType<BaseKtLintCheckTask>().forEach { task ->
            task.setSource(
                fileTree(task.project.projectDir) {
                    include("src/**/*.kt", "src/**/*.kts")
                },
            )
        }
    }
}
