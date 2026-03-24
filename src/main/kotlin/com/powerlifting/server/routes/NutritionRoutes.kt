package com.powerlifting.server.routes

import com.powerlifting.server.domain.usecase.nutrition.AddNutritionEntryUseCase
import com.powerlifting.server.domain.usecase.nutrition.DeleteNutritionEntryUseCase
import com.powerlifting.server.domain.usecase.nutrition.GetTodayNutritionUseCase
import com.powerlifting.server.domain.usecase.nutrition.UpdateNutritionGoalsUseCase
import com.powerlifting.server.dto.CreateNutritionEntryRequest
import com.powerlifting.server.dto.UpdateNutritionGoalsRequest
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
import java.time.ZoneOffset
import java.util.UUID

fun Route.registerNutritionRoutes(
    getTodayNutrition: GetTodayNutritionUseCase,
    updateNutritionGoals: UpdateNutritionGoalsUseCase,
    addNutritionEntry: AddNutritionEntryUseCase,
    deleteNutritionEntry: DeleteNutritionEntryUseCase
) {
    route("/nutrition") {
        put("/goals") {
            val u = call.userRow()
            val req = call.receive<UpdateNutritionGoalsRequest>()
            val updated = updateNutritionGoals(u.id, req.toDomain())
            call.respond(updated.toDto())
        }

        get("/today") {
            val u = call.userRow()
            val date = call.request.queryParameters["date"]?.let { LocalDate.parse(it) }
                ?: LocalDate.now(ZoneOffset.UTC)

            val day = getTodayNutrition(u.id, date)
            call.respond(day.toResponse())
        }

        post("/entries") {
            val u = call.userRow()
            val req = call.receive<CreateNutritionEntryRequest>()
            val created = addNutritionEntry(u.id, req.toDomain())
            call.respond(HttpStatusCode.Created, created.toDto())
        }

        delete("/entries/{id}") {
            val u = call.userRow()
            val idStr = call.parameters["id"] ?: throw IllegalArgumentException("Missing id")
            val id = UUID.fromString(idStr)

            val ok = deleteNutritionEntry(u.id, id)
            if (ok) call.respond(HttpStatusCode.NoContent) else call.respond(HttpStatusCode.NotFound)
        }
    }
}
