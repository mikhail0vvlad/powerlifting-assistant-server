package com.powerlifting.server.routes

import com.powerlifting.server.domain.usecase.program.GenerateProgramUseCase
import com.powerlifting.server.domain.usecase.program.GetActiveProgramUseCase
import com.powerlifting.server.domain.usecase.program.GetProgramCalendarUseCase
import com.powerlifting.server.domain.usecase.program.RescheduleWorkoutUseCase
import com.powerlifting.server.domain.usecase.program.SkipWorkoutUseCase
import com.powerlifting.server.dto.GenerateProgramRequest
import com.powerlifting.server.dto.RescheduleWorkoutRequest
import com.powerlifting.server.routes.mapper.emptyCalendarResponse
import com.powerlifting.server.routes.mapper.toDomain
import com.powerlifting.server.routes.mapper.toDto
import com.powerlifting.server.routes.mapper.toResponse
import com.powerlifting.server.userRow
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDate
import java.util.UUID

fun Route.registerProgramRoutes(
    generateProgram: GenerateProgramUseCase,
    getActiveProgram: GetActiveProgramUseCase,
    getProgramCalendar: GetProgramCalendarUseCase,
    rescheduleWorkout: RescheduleWorkoutUseCase,
    skipWorkout: SkipWorkoutUseCase
) {
    route("/programs") {
        post("/generate") {
            val u = call.userRow()
            val req = call.receive<GenerateProgramRequest>()
            val program = generateProgram(u.id, req.toDomain())
            call.respond(HttpStatusCode.Created, program.toDto())
        }

        get("/active") {
            val u = call.userRow()
            val upcomingLimit = call.request.queryParameters["upcomingLimit"]?.toIntOrNull()
                ?.coerceIn(1, 50)
                ?: 10
            call.respond(getActiveProgram(u.id, upcomingLimit).toResponse())
        }

        post("/workouts/{id}/reschedule") {
            val u = call.userRow()
            val workoutId = workoutIdParam(call.parameters["id"])
            val req = call.receive<RescheduleWorkoutRequest>()
            val newDate = runCatching { LocalDate.parse(req.newDate) }.getOrNull()
                ?: throw IllegalArgumentException("Invalid newDate (expected YYYY-MM-DD)")
            val moved = rescheduleWorkout(u.id, workoutId, newDate)
            call.respond(HttpStatusCode.OK, moved.toDto())
        }

        post("/workouts/{id}/skip") {
            val u = call.userRow()
            val workoutId = workoutIdParam(call.parameters["id"])
            skipWorkout(u.id, workoutId)
            call.respond(HttpStatusCode.NoContent)
        }
    }

    get("/calendar") {
        val u = call.userRow()
        val from = call.request.queryParameters["from"]?.let { LocalDate.parse(it) }
        val to = call.request.queryParameters["to"]?.let { LocalDate.parse(it) }

        val calendar = getProgramCalendar(u.id, from, to)
        if (calendar == null) {
            call.respond(emptyCalendarResponse())
        } else {
            call.respond(calendar.toResponse())
        }
    }
}

private fun workoutIdParam(raw: String?): UUID =
    UUID.fromString(raw ?: throw IllegalArgumentException("Missing id"))
