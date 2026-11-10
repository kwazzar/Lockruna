package com.kwazzar.lockruna.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Guards presentation boundaries: stores use application use cases only, and
 * platform implementations remain outside the shared presentation layer.
 */
class PresentationLayerTest {
    private val scope: KoScope = Konsist.scopeFromDirectories(listOf("shared"))

    private val productionFiles
        get() = scope.files.filter { file ->
            file.projectPath.contains("/presentation/") &&
                !file.projectPath.contains("/commonTest/") &&
                !file.projectPath.contains("/jvmTest/")
        }

    @Test
    fun `presentation module exists`() {
        val moduleFiles = productionFiles.filter { file ->
            file.projectPath.endsWith("/presentation/di/PresentationModule.kt")
        }

        assertTrue(moduleFiles.isNotEmpty(), "presentation DI module was not found")
    }

    @Test
    fun `presentation sources do not import frameworks or repositories`() {
        val forbidden = listOf(
            "app.cash.sqldelight.",
            "io.ktor.",
            "androidx.",
            "com.kwazzar.lockruna.domain.vault.VaultItemRepository",
        )
        val presentationFiles = productionFiles

        assertTrue(presentationFiles.isNotEmpty(), "presentation sources were not found")
        presentationFiles.forEach { file ->
            val forbiddenImports = file.imports
                .filter { declaration -> forbidden.any { declaration.name.startsWith(it) } }
                .map { declaration -> declaration.name }

            assertTrue(
                forbiddenImports.isEmpty(),
                "presentation file ${file.projectPath} imports forbidden dependencies: $forbiddenImports",
            )
        }
    }

    @Test
    fun `stores do not depend on the data layer`() {
        val storeFiles = productionFiles.filter { file ->
            file.projectPath.endsWith("Store.kt")
        }

        assertTrue(storeFiles.isNotEmpty(), "presentation stores were not found")
        storeFiles.forEach { file ->
            val dataImports = file.imports
                .filter { declaration -> declaration.name.startsWith("com.kwazzar.lockruna.data.") }
                .map { declaration -> declaration.name }

            assertTrue(
                dataImports.isEmpty(),
                "presentation store ${file.projectPath} imports data: $dataImports",
            )
        }
    }
}
