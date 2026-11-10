package com.kwazzar.lockruna.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Guards the sync feature boundaries: the domain owns the exchange types and the ports,
 * the data layer talks to the network and the database, and the application orchestrates them.
 */
class SyncLayerTest {
    private val scope: KoScope = Konsist.scopeFromDirectories(listOf("shared"))

    @Test
    fun `domain sync package does not depend on frameworks`() {
        val forbidden = listOf("io.ktor.", "app.cash.sqldelight.", "kotlinx.serialization.")
        val domainSyncFiles = scope.files.filter { file -> file.projectPath.contains("/domain/sync/") }

        assertTrue(domainSyncFiles.isNotEmpty(), "domain sync sources were not found")
        domainSyncFiles.forEach { file ->
            val frameworkImports = file.imports
                .filter { declaration -> forbidden.any { declaration.name.startsWith(it) } }
                .map { declaration -> declaration.name }

            assertTrue(
                frameworkImports.isEmpty(),
                "domain sync file ${file.projectPath} imports frameworks: $frameworkImports",
            )
        }
    }

    @Test
    fun `data sync package does not depend on presentation`() {
        val dataSyncFiles = scope.files.filter { file -> file.projectPath.contains("/data/sync/") }

        assertTrue(dataSyncFiles.isNotEmpty(), "data sync sources were not found")
        dataSyncFiles.forEach { file ->
            val presentationImports = file.imports
                .filter { declaration -> declaration.name.startsWith("com.kwazzar.lockruna.presentation.") }
                .map { declaration -> declaration.name }

            assertTrue(
                presentationImports.isEmpty(),
                "data sync file ${file.projectPath} imports presentation: $presentationImports",
            )
        }
    }

    @Test
    fun `application sync package does not depend on frameworks`() {
        val forbidden = listOf("io.ktor.", "app.cash.sqldelight.")
        val applicationSyncFiles = scope.files.filter { file -> file.projectPath.contains("/application/sync/") }

        assertTrue(applicationSyncFiles.isNotEmpty(), "application sync sources were not found")
        applicationSyncFiles.forEach { file ->
            val frameworkImports = file.imports
                .filter { declaration -> forbidden.any { declaration.name.startsWith(it) } }
                .map { declaration -> declaration.name }

            assertTrue(
                frameworkImports.isEmpty(),
                "application sync file ${file.projectPath} imports frameworks: $frameworkImports",
            )
        }
    }
}
