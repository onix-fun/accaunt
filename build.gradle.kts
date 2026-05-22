plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.serialization") version "2.3.21"
    application
}

group = "profile"
version = "1.0.0-SNAPSHOT"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

kotlin {
    jvmToolchain(21)
}

tasks.register<Exec>("dockerComposeUp") {
    group = "application"
    description = "Starts docker-compose services."
    commandLine("docker-compose", "up", "-d")
}

tasks.named<JavaExec>("run") {
    dependsOn("dockerComposeUp")
}

dependencies {
    // Ktor Core & Plugins
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.auth)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.cors)
    implementation(ktorLibs.server.statusPages)
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.serialization.kotlinx.json)
    
    // Documentation
    implementation(ktorLibs.server.openapi)

    // Database & Migrations
    implementation(libs.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)

    // Redis
    implementation(libs.lettuce.core)

    // Security
    implementation(libs.argon2.jvm)
    implementation(libs.java.jwt)

    // Storage
    implementation(libs.minio)

    // Logging
    implementation(libs.logback.classic)

    // Testing
    testImplementation(libs.h2)
    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
    testImplementation(ktorLibs.client.contentNegotiation)
    testImplementation(ktorLibs.serialization.kotlinx.json)
}
