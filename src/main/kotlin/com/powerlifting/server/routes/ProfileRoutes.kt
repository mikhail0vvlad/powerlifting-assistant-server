package com.powerlifting.server.routes

import com.powerlifting.server.domain.usecase.profile.GetProfileSummaryUseCase
import com.powerlifting.server.domain.usecase.profile.UpdateProfileUseCase
import com.powerlifting.server.dto.UpdateProfileRequest
import com.powerlifting.server.routes.mapper.toDomain
import com.powerlifting.server.routes.mapper.toDto
import com.powerlifting.server.routes.mapper.toResponse
import com.powerlifting.server.userRow
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.registerProfileRoutes(
    getProfileSummary: GetProfileSummaryUseCase,
    updateProfile: UpdateProfileUseCase
) {
    route("/profile") {
        get {
            val summary = getProfileSummary(call.userRow())
            call.respond(summary.toResponse())
        }

        put {
            val u = call.userRow()
            val req = call.receive<UpdateProfileRequest>()
            val updated = updateProfile(u.id, req.toDomain())
            call.respond(updated.toDto())
        }
    }
}
