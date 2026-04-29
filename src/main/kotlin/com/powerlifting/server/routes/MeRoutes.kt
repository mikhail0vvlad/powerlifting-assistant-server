package com.powerlifting.server.routes

import com.powerlifting.server.routes.mapper.toMeResponse
import com.powerlifting.server.userRow
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.registerMeRoutes() {
    get("/me") {
        call.respond(call.userRow().toMeResponse())
    }
}
