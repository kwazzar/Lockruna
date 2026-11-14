import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktor)
    application
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass.set("com.kwazzar.lockruna.server.ApplicationKt")
}

tasks.test {
    environment("TEST_DATABASE_URL", System.getenv("TEST_DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/lockruna_test")
    environment("TEST_DATABASE_USER", System.getenv("TEST_DATABASE_USER") ?: "lockruna")
    environment("TEST_DATABASE_PASSWORD", System.getenv("TEST_DATABASE_PASSWORD") ?: "lockruna_password")
    environment("TEST_MONGODB_URI", System.getenv("TEST_MONGODB_URI") ?: "mongodb://lockruna:lockruna_password@localhost:27017/?authSource=admin")
    environment("DOCKER_HOST", System.getenv("DOCKER_HOST") ?: "unix:///Users/nazar/.colima/default/docker.sock")
    environment("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE", System.getenv("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE") ?: "/Users/nazar/.colima/default/docker.sock")
}

dependencies {
    implementation(project(":api-contract"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.serialization.json)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.koin.core)
    implementation(libs.koin.ktor)
    implementation(libs.java.jwt)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation(libs.exposed.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.lettuce.core)
    implementation(libs.micrometer.core)
    implementation(libs.postgresql)
    implementation(libs.mongodb.driver.kotlin.coroutine)
    runtimeOnly(libs.logback.classic)

    testImplementation(kotlin("test"))
    testImplementation(libs.logback.classic)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.mongodb)
    testImplementation(libs.testcontainers.junit)
}
