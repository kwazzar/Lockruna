package com.kwazzar.lockruna.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope
import kotlin.test.Test
import kotlin.test.assertTrue

class AuthLayerTest {

    private val scope: KoScope = Konsist.scopeFromDirectories(listOf("shared", "api-contract"))

    @Test
    fun `domain auth layer should not import ktor, sqldelight, or serialization`() {
        val domainAuthFiles = scope.files.filter { file ->
            file.projectPath.contains("/domain/auth/")
        }

        assertTrue(domainAuthFiles.isNotEmpty(), "domain auth sources were not found")
        domainAuthFiles.forEach { file ->
            val forbiddenImports = file.imports
                .filter { declaration ->
                    declaration.name.startsWith("io.ktor.") ||
                        declaration.name.startsWith("app.cash.sqldelight.") ||
                        declaration.name.startsWith("kotlinx.serialization.")
                }
                .map { declaration -> declaration.name }

            assertTrue(
                forbiddenImports.isEmpty(),
                "domain auth file ${file.projectPath} imports forbidden libraries: $forbiddenImports",
            )
        }
    }

    @Test
    fun `application auth layer should not import ktor or sqldelight`() {
        val appAuthFiles = scope.files.filter { file ->
            file.projectPath.contains("/application/auth/")
        }

        assertTrue(appAuthFiles.isNotEmpty(), "application auth sources were not found")
        appAuthFiles.forEach { file ->
            val forbiddenImports = file.imports
                .filter { declaration ->
                    declaration.name.startsWith("io.ktor.") ||
                        declaration.name.startsWith("app.cash.sqldelight.")
                }
                .map { declaration -> declaration.name }

            assertTrue(
                forbiddenImports.isEmpty(),
                "application auth file ${file.projectPath} imports forbidden libraries: $forbiddenImports",
            )
        }
    }

    @Test
    fun `data auth layer should not import presentation`() {
        val dataAuthFiles = scope.files.filter { file ->
            file.projectPath.contains("/data/auth/")
        }

        assertTrue(dataAuthFiles.isNotEmpty(), "data auth sources were not found")
        dataAuthFiles.forEach { file ->
            val presentationImports = file.imports
                .filter { declaration -> declaration.name.startsWith("com.kwazzar.lockruna.presentation.") }
                .map { declaration -> declaration.name }

            assertTrue(
                presentationImports.isEmpty(),
                "data auth file ${file.projectPath} imports presentation: $presentationImports",
            )
        }
    }
}
