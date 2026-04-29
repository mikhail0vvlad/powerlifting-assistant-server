package com.powerlifting.server.routes

import com.powerlifting.server.domain.usecase.workout.AddWorkoutSetsUseCase
import com.powerlifting.server.domain.usecase.workout.DeleteWorkoutSessionUseCase
import com.powerlifting.server.domain.usecase.workout.FinishWorkoutSessionUseCase
import com.powerlifting.server.domain.usecase.workout.GetWorkoutHistoryUseCase
import com.powerlifting.server.domain.usecase.workout.GetWorkoutSessionDetailUseCase
import com.powerlifting.server.domain.usecase.workout.StartWorkoutSessionUseCase
import com.powerlifting.server.dto.AddWorkoutSetsRequest
import com.powerlifting.server.dto.FinishWorkoutWithRatingRequest
import com.powerlifting.server.dto.StartWorkoutSessionRequest
import com.powerlifting.server.routes.mapper.decodeCursor
import com.powerlifting.server.routes.mapper.toDomain
import com.powerlifting.server.routes.mapper.toResponse
import com.powerlifting.server.userRow
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.registerWorkoutRoutes(
    startWorkoutSession: StartWorkoutSessionUseCase,
    addWorkoutSets: AddWorkoutSetsUseCase,
    finishWorkoutSession: FinishWorkoutSessionUseCase,
    getWorkoutSessionDetail: GetWorkoutSessionDetailUseCase,
    getWorkoutHistory: GetWorkoutHistoryUseCase,
    deleteWorkoutSession: DeleteWorkoutSessionUseCase
) {
    route("/workouts") {
        route("/sessions") {
            post("/start") {
                val u = call.userRow()
                val req = call.receive<StartWorkoutSessionRequest>()
                val started = startWorkoutSession(u.id, req.toDomain())
                call.respond(HttpStatusCode.Created, started.toResponse())
            }

            post("/{id}/sets") {
                val u = call.userRow()
                val sessionId = sessionIdParam(call.parameters["id"])
                val req = call.receive<AddWorkoutSetsRequest>()

                addWorkoutSets(u.id, sessionId, req.sets.map { it.toDomain() })
                call.respond(HttpStatusCode.NoContent)
            }

            post("/{id}/finish") {
                val u = call.userRow()
                val sessionId = sessionIdParam(call.parameters["id"])
                val req = call.receive<FinishWorkoutWithRatingRequest>()

                finishWorkoutSession(u.id, sessionId, req.workoutDurationSec, req.wellbeingRating)
                call.respond(HttpStatusCode.NoContent)
            }

            get("/{id}") {
                val u = call.userRow()
                val sessionId = sessionIdParam(call.parameters["id"])
                val detail = getWorkoutSessionDetail(u.id, sessionId)
                    ?: throw IllegalArgumentException("Session not found")
                call.respond(detail.toResponse())
            }

            delete("/{id}") {
                val u = call.userRow()
                val sessionId = sessionIdParam(call.parameters["id"])
                val ok = deleteWorkoutSession(u.id, sessionId)
                if (ok) call.respond(HttpStatusCode.NoContent) else call.respond(HttpStatusCode.NotFound)
            }
        }

        get("/history") {
            val u = call.userRow()
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 30
            val before = call.request.queryParameters["cursor"]?.let {
                try {
                    decodeCursor(it)
                } catch (_: Exception) {
                    throw IllegalArgumentException("Invalid cursor")
                }
            }
            val page = getWorkoutHistory(u.id, before, limit)
            call.respond(page.toResponse())
        }
    }
}

private fun sessionIdParam(raw: String?): UUID =
    UUID.fromString(raw ?: throw IllegalArgumentException("Missing id"))
