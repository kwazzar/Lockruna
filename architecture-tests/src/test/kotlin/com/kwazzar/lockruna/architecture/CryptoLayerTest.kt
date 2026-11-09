package com.kwazzar.lockruna.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Guards the crypto layer: the domain only declares ports, the data layer
 * implements them, and neither leaks crypto libraries into the domain.
 */
class CryptoLayerTest {
    private val scope: KoScope = Konsist.scopeFromDirectories(listOf("shared"))

    @Test
    fun `domain crypto ports do not depend on crypto libraries`() {
        val forbidden = listOf("org.bouncycastle.", "com.ionspin.", "io.ktor.", "app.cash.sqldelight.", "kotlinx.")
        val domainCryptoFiles = scope.files.filter { file -> file.projectPath.contains("/domain/crypto/") }

        assertTrue(domainCryptoFiles.isNotEmpty(), "domain crypto sources were not found")
        domainCryptoFiles.forEach { file ->
            val cryptoImports = file.imports
                .filter { declaration -> forbidden.any { declaration.name.startsWith(it) } }
                .map { declaration -> declaration.name }

            assertTrue(
                cryptoImports.isEmpty(),
                "domain crypto file ${file.projectPath} imports crypto libraries: $cryptoImports",
            )
        }
    }

    @Test
    fun `data crypto implementations do not depend on presentation`() {
        val dataCryptoFiles = scope.files.filter { file -> file.projectPath.contains("/data/crypto/") }

        assertTrue(dataCryptoFiles.isNotEmpty(), "data crypto sources were not found")
        dataCryptoFiles.forEach { file ->
            val presentationImports = file.imports
                .filter { declaration -> declaration.name.startsWith("com.kwazzar.lockruna.presentation.") }
                .map { declaration -> declaration.name }

            assertTrue(
                presentationImports.isEmpty(),
                "data crypto file ${file.projectPath} imports presentation: $presentationImports",
            )
        }
    }

    @Test
    fun `domain crypto has no generic Base abstractions`() {
        val baseDeclarations = scope.classesAndInterfacesAndObjects()
            .filter { declaration -> declaration.projectPath.contains("/domain/crypto/") }
            .filter { declaration -> declaration.name.startsWith("Base") }
            .map { declaration -> declaration.projectPath }

        assertTrue(baseDeclarations.isEmpty(), "generic Base abstractions found: $baseDeclarations")
    }
}
