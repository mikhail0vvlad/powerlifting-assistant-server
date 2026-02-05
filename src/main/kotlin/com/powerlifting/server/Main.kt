package com.powerlifting.server

import com.powerlifting.server.config.ConfigLoader
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    val config = ConfigLoader.loadFromEnv()
    embeddedServer(Netty, port = config.port, host = "0.0.0.0") {
        module(config)
    }.start(wait = true)
}
