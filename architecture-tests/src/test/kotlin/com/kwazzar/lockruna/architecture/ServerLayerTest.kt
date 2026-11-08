package com.kwazzar.lockruna.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Guards the server layer boundaries from ADR-004: the api layer talks to use cases only,
 * the domain stays free of frameworks and the infrastructure knows nothing about routing.
 */
class ServerLayerTest {
    private val scope: KoScope = Konsist.scopeFromDirectory("server/src/main/kotlin")

    @Test
    fun `api layers do not import the infrastructure frameworks`() {
        val frameworks = listOf("org.jetbrains.exposed.", "com.zaxxer.hikari.", "io.lettuce.")
        val apiFiles = scope.files.filter { file ->
            file.projectPath.contains("/identity/api/") || file.projectPath.contains("/sync/api/")
        }

        assertTrue(apiFiles.isNotEmpty(), "server api sources were not found")
        apiFiles.forEach { file ->
            val imports = file.imports
                .map { declaration -> declaration.name }
                .filter { name -> frameworks.any { name.startsWith(it) } }

            assertTrue(
                imports.isEmpty(),
                "api file ${file.projectPath} imports infrastructure frameworks: $imports",
            )
        }
    }

    @Test
    fun `server domain has no framework imports`() {
        val frameworks = listOf("io.ktor.", "org.jetbrains.exposed.", "io.lettuce.")
        val domainFiles = scope.files.filter { file -> file.projectPath.contains("/domain/") }

        assertTrue(domainFiles.isNotEmpty(), "server domain sources were not found")
        domainFiles.forEach { file ->
            val imports = file.imports
                .map { declaration -> declaration.name }
                .filter { name -> frameworks.any { name.startsWith(it) } }

            assertTrue(
                imports.isEmpty(),
                "domain file ${file.projectPath} imports frameworks: $imports",
            )
        }
    }

    @Test
    fun `server infrastructure does not know the routing`() {
        val routing = "io.ktor.server.routing."
        val infrastructureFiles = scope.files.filter { file -> file.projectPath.contains("/infrastructure/") }

        assertTrue(infrastructureFiles.isNotEmpty(), "server infrastructure sources were not found")
        infrastructureFiles.forEach { file ->
            val imports = file.imports
                .map { declaration -> declaration.name }
                .filter { name -> name.startsWith(routing) }

            assertTrue(
                imports.isEmpty(),
                "infrastructure file ${file.projectPath} imports routing: $imports",
            )
        }
    }
}
