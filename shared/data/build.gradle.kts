import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    android {
        namespace = "com.kwazzar.lockruna.data"
        compileSdk = 37
        minSdk = 28

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    iosArm64 {
        compilations.getByName("main").cinterops {
            create("icu") {
                definitionFile.set(project.file("src/iosMain/cinterop/icu.def"))
                includeDirs(project.file("src/iosMain/cinterop"))
            }
        }
    }
    iosSimulatorArm64 {
        compilations.getByName("main").cinterops {
            create("icu") {
                definitionFile.set(project.file("src/iosMain/cinterop/icu.def"))
                includeDirs(project.file("src/iosMain/cinterop"))
            }
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // A custom group keeps the JVM and Android sources shared without disabling the
    // default hierarchy template: explicit dependsOn edges would detach iosMain.
    applyDefaultHierarchyTemplate {
        common {
            group("jvmAndAndroid") {
                withJvm()
                // The Android KMP plugin does not use the KGP Android target type.
                withCompilations { compilation -> compilation.target.name == "android" }
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":shared:application"))
            api(project(":api-contract"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.koin.core)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.driver.sqlite)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.driver.android)
        }
        iosMain.dependencies {
            implementation(libs.ionspin.libsodium)
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.driver.native)
        }
        getByName("jvmAndAndroidMain").dependencies {
            implementation(libs.bouncycastle)
        }

        // The iOS sources use cinterop packages (libsodium, ICU), which the shared
        // native metadata compilation cannot resolve, so they are compiled per target.
        getByName("iosMain").kotlin.setSrcDirs(emptyList<File>())
        getByName("iosArm64Main").kotlin.srcDir("src/iosMain/kotlin")
        getByName("iosSimulatorArm64Main").kotlin.srcDir("src/iosMain/kotlin")
    }
}

sqldelight {
    databases {
        create("PassKeeperDatabase") {
            packageName.set("com.kwazzar.lockruna.data.db")
            srcDirs.setFrom("src/commonMain/sqldelight")
        }
    }
}
