package com.powerlifting.server.routes

import com.powerlifting.server.domain.usecase.achievements.CreateAchievementUseCase
import com.powerlifting.server.domain.usecase.achievements.DeleteAchievementUseCase
import com.powerlifting.server.domain.usecase.achievements.ListAchievementsUseCase
import com.powerlifting.server.dto.CreateAchievementRequest
import com.powerlifting.server.routes.mapper.toDomain
import com.powerlifting.server.routes.mapper.toDto
import com.powerlifting.server.userRow
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.registerAchievementRoutes(
    listAchievements: ListAchievementsUseCase,
    createAchievement: CreateAchievementUseCase,
    deleteAchievement: DeleteAchievementUseCase
) {
    route("/achievements") {
        get {
            val u = call.userRow()
            // Cap both to prevent slow OFFSET scans and huge response payloads.
            // TODO(perf): migrate to keyset cursor on `created_at DESC` like
            // /workouts/history once the Android client paginates.
            val offset = (call.request.queryParameters["offset"]?.toIntOrNull() ?: 0)
                .coerceIn(0, 10_000)
            val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: 50)
                .coerceIn(1, 100)
            val list = listAchievements(u.id, offset, limit)
            call.respond(list.map { it.toDto() })
        }

        post {
            val u = call.userRow()
            val req = call.receive<CreateAchievementRequest>()
            val created = createAchievement(u.id, req.toDomain())
            call.respond(HttpStatusCode.Created, created.toDto())
        }

        delete("/{id}") {
            val u = call.userRow()
            val id = parseUuidOrBadRequest(call.parameters["id"], "achievement id")
            val ok = deleteAchievement(u.id, id)
            if (ok) call.respond(HttpStatusCode.NoContent) else call.respond(HttpStatusCode.NotFound)
        }
    }
}
