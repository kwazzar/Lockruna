import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.sqldelight)
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

    iosArm64()
    iosSimulatorArm64()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // A custom group keeps the JVM and Android sources shared without disabling the
    // default hierarchy template: explicit dependsOn edges would detach iosMain.
    applyDefaultHierarchyTemplate {
        common {
            group("jvmAndAndroid") {
                withJvm()
                withAndroidTarget()
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":shared:application"))
            api(project(":api-contract"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.koin.core)
            implementation(libs.sqldelight.runtime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
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
