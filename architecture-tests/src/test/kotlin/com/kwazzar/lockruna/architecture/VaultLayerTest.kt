package com.kwazzar.lockruna.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Guards the vault feature boundaries: the domain owns aggregates and ports, the
 * application orchestrates them, and the data layer stays free of presentation.
 */
class VaultLayerTest {
    private val scope: KoScope = Konsist.scopeFromDirectories(listOf("shared"))

    @Test
    fun `domain vault and session packages do not depend on frameworks`() {
        val forbidden = listOf("app.cash.sqldelight.", "io.ktor.", "kotlinx.serialization.", "org.jetbrains.exposed.")
        val vaultAndSessionFiles = scope.files.filter { file ->
            file.projectPath.contains("/domain/vault/") || file.projectPath.contains("/domain/session/")
        }

        assertTrue(vaultAndSessionFiles.isNotEmpty(), "vault domain sources were not found")
        vaultAndSessionFiles.forEach { file ->
            val frameworkImports = file.imports
                .filter { declaration -> forbidden.any { declaration.name.startsWith(it) } }
                .map { declaration -> declaration.name }

            assertTrue(
                frameworkImports.isEmpty(),
                "domain vault/session file ${file.projectPath} imports frameworks: $frameworkImports",
            )
        }
    }

    @Test
    fun `application vault and session packages do not depend on data`() {
        val vaultAndSessionFiles = scope.files.filter { file ->
            file.projectPath.contains("/application/vault/") || file.projectPath.contains("/application/session/")
        }

        assertTrue(vaultAndSessionFiles.isNotEmpty(), "application vault/session sources were not found")
        vaultAndSessionFiles.forEach { file ->
            val dataImports = file.imports
                .filter { declaration -> declaration.name.startsWith("com.kwazzar.lockruna.data.") }
                .map { declaration -> declaration.name }

            assertTrue(
                dataImports.isEmpty(),
                "application vault/session file ${file.projectPath} imports data: $dataImports",
            )
        }
    }

    @Test
    fun `data db package does not depend on presentation`() {
        val dataDbFiles = scope.files.filter { file -> file.projectPath.contains("/data/db/") }

        assertTrue(dataDbFiles.isNotEmpty(), "data db sources were not found")
        dataDbFiles.forEach { file ->
            val presentationImports = file.imports
                .filter { declaration -> declaration.name.startsWith("com.kwazzar.lockruna.presentation.") }
                .map { declaration -> declaration.name }

            assertTrue(
                presentationImports.isEmpty(),
                "data db file ${file.projectPath} imports presentation: $presentationImports",
            )
        }
    }

    @Test
    fun `vault domain has no generic Base abstractions`() {
        val baseDeclarations = scope.classesAndInterfacesAndObjects()
            .filter { declaration -> declaration.name.startsWith("Base") }
            .filter { declaration ->
                declaration.projectPath.contains("/domain/vault/") ||
                    declaration.projectPath.contains("/domain/session/")
            }
            .map { declaration -> declaration.projectPath }

        assertTrue(baseDeclarations.isEmpty(), "generic Base abstractions found: $baseDeclarations")
    }
}
