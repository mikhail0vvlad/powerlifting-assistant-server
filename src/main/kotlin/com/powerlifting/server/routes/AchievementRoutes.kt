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
            val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
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
            val idStr = call.parameters["id"] ?: throw IllegalArgumentException("Missing id")
            val id = UUID.fromString(idStr)
            val ok = deleteAchievement(u.id, id)
            if (ok) call.respond(HttpStatusCode.NoContent) else call.respond(HttpStatusCode.NotFound)
        }
    }
}
