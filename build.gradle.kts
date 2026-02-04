import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.serialization") version "1.9.24"
    application
}

group = "com.powerlifting"
version = "1.0.0"

repositories {
    mavenCentral()
}

val ktorVersion = "2.3.7"
val exposedVersion = "0.50.1"

application {
    mainClass.set("com.powerlifting.server.MainKt")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Ktor
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-cors-jvm:$ktorVersion")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.6")

    // DB
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("org.flywaydb:flyway-core:11.0.1")
    implementation("org.flywaydb:flyway-database-postgresql:11.0.1")

    // Exposed
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    // Firebase Admin (verify Firebase ID tokens)
    implementation("com.google.firebase:firebase-admin:9.3.0")

    // Caching (in-process, no Redis needed for single-instance VPS deployment)
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // Tests
    testImplementation(kotlin("test-junit5"))
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

// Ensure UTF-8
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs += listOf("-Xjsr305=strict")
    }
}
