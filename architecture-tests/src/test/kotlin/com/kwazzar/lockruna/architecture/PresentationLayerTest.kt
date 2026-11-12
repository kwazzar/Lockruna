package com.kwazzar.lockruna.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Guards presentation boundaries: stores use application use cases only, state
 * carries the secret-free UI model, and platform implementations stay out.
 */
class PresentationLayerTest {
    private val scope: KoScope = Konsist.scopeFromDirectories(listOf("shared"))

    private val productionFiles
        get() = scope.files.filter { file ->
            file.projectPath.contains("/presentation/") &&
                !file.projectPath.contains("/commonTest/") &&
                !file.projectPath.contains("/jvmTest/")
        }

    private val storeFiles
        get() = productionFiles.filter { file -> file.projectPath.endsWith("Store.kt") }

    @Test
    fun `presentation module exists`() {
        val moduleFiles = productionFiles.filter { file ->
            file.projectPath.endsWith("/presentation/di/PresentationModule.kt")
        }

        assertTrue(moduleFiles.isNotEmpty(), "presentation DI module was not found")
    }

    @Test
    fun `presentation sources do not import frameworks`() {
        val forbidden = listOf("app.cash.sqldelight.", "io.ktor.", "androidx.")
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
    fun `stores depend on application use cases only`() {
        val forbidden = listOf(
            "com.kwazzar.lockruna.data.",
            "com.kwazzar.lockruna.domain.vault.VaultItemRepository",
            "com.kwazzar.lockruna.domain.vault.Clipboard",
            "app.cash.sqldelight.",
            "io.ktor.",
            "androidx.",
        )
        val stores = storeFiles

        assertTrue(stores.isNotEmpty(), "presentation stores were not found")
        stores.forEach { file ->
            val forbiddenImports = file.imports
                .filter { declaration -> forbidden.any { declaration.name.startsWith(it) } }
                .map { declaration -> declaration.name }

            assertTrue(
                forbiddenImports.isEmpty(),
                "presentation store ${file.projectPath} imports forbidden dependencies: $forbiddenImports",
            )

            val useCaseImports = file.imports
                .filter { declaration -> declaration.name.startsWith("com.kwazzar.lockruna.application.") }

            assertTrue(
                useCaseImports.isNotEmpty(),
                "presentation store ${file.projectPath} does not use application use cases",
            )
        }
    }

    @Test
    fun `state never holds aggregates or secrets`() {
        // The aggregate and the secret value object. The VaultItemType enum carries no
        // data, and ItemUi already projects it to the UI, so only the exact names are banned.
        val forbiddenTypes = listOf("VaultItem", "Secret")
        val states = scope.classesAndInterfacesAndObjects().filter { declaration ->
            declaration.name.endsWith("State") && declaration.projectPath.contains("/presentation/")
        }

        assertTrue(states.isNotEmpty(), "presentation states were not found")
        states.forEach { state ->
            val forbiddenProperties = state.properties()
                .map { property -> property.type?.name.orEmpty() }
                .filter { typeName -> forbiddenTypes.any { it == typeName } }

            assertTrue(
                forbiddenProperties.isEmpty(),
                "presentation state ${state.projectPath} holds forbidden types: $forbiddenProperties",
            )
        }
    }

    @Test
    fun `state never names a secret property`() {
        val forbiddenNames = listOf("password", "secret", "vaultkey")
        val states = scope.classesAndInterfacesAndObjects().filter { declaration ->
            declaration.name.endsWith("State") && declaration.projectPath.contains("/presentation/")
        }

        assertTrue(states.isNotEmpty(), "presentation states were not found")
        states.forEach { state ->
            val forbiddenProperties = state.properties()
                .map { property -> property.name }
                .filter { name -> forbiddenNames.any { it.equals(name, ignoreCase = true) } }

            assertTrue(
                forbiddenProperties.isEmpty(),
                "presentation state ${state.projectPath} holds secret properties: $forbiddenProperties",
            )
        }
    }
}
