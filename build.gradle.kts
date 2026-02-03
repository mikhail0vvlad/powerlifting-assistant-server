plugins {
    kotlin("jvm") version "1.9.22"
    application
}
repositories { mavenCentral() }
dependencies {
    implementation("io.ktor:ktor-server-core:2.3.7")
    implementation("io.ktor:ktor-server-netty:2.3.7")
}
application { mainClass.set("com.powerlifting.server.MainKt") }
