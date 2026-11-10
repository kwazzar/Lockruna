package com.kwazzar.lockruna.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator
import com.lemonappdev.konsist.api.architecture.Layer
import com.lemonappdev.konsist.api.container.KoScope
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Guards the layer boundaries from AGENTS.md: dependencies point inward and the
 * domain stays free of framework imports.
 */
class LayerDependencyTest {
    private val scope: KoScope = Konsist.scopeFromDirectories(listOf("shared", "api-contract"))

    @Test
    fun `layers only depend on the layers below them`() {
        with(KoArchitectureCreator) {
            scope.assertArchitecture {
                val domain = Layer("Domain", "com.kwazzar.lockruna.domain..")
                val application = Layer("Application", "com.kwazzar.lockruna.application..")
                val presentation = Layer("Presentation", "com.kwazzar.lockruna.presentation..")
                val data = Layer("Data", "com.kwazzar.lockruna.data..")
                val contract = Layer("Contract", "com.kwazzar.lockruna.contract..")
                val umbrella = Layer("Umbrella", "com.kwazzar.lockruna.umbrella..")

                domain.dependsOnNothing()
                application.dependsOn(domain)
                presentation.dependsOn(domain, application)
                data.dependsOn(domain, application, contract)
                umbrella.include()
            }
        }
    }

    @Test
    fun `domain has no framework imports`() {
        val frameworks =
            listOf("org.jetbrains.", "io.ktor.", "app.cash.sqldelight.", "kotlinx.serialization.", "kotlinx.datetime.")
        val domainFiles = scope.files.filter { file -> file.projectPath.startsWith("/shared/domain/") }

        assertTrue(domainFiles.isNotEmpty(), "domain sources were not found")
        domainFiles.forEach { file ->
            val frameworkImports = file.imports
                .filter { declaration -> frameworks.any { declaration.name.startsWith(it) } }
                .map { declaration -> declaration.name }

            assertTrue(
                frameworkImports.isEmpty(),
                "domain file ${file.projectPath} imports frameworks: $frameworkImports",
            )
        }
    }

    @Test
    fun `no generic Base abstractions exist`() {
        val baseDeclarations = scope.classesAndInterfacesAndObjects()
            .filter { declaration -> declaration.name.startsWith("Base") }
            .map { declaration -> declaration.projectPath }

        assertTrue(baseDeclarations.isEmpty(), "generic Base abstractions found: $baseDeclarations")
    }
}
