plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.skie)
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
            binaryOption("bundleId", "com.kwazzar.lockruna.shared")
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":shared:data"))
            api(project(":shared:presentation"))
            api(libs.koin.core)
        }
    }
}
