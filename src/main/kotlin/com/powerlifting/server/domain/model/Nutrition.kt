package com.powerlifting.server.domain.model

import java.time.Instant
import java.util.UUID

data class NutritionEntry(
    val id: UUID,
    val title: String,
    val eatenAt: Instant,
    val calories: Int,
    val proteinG: Int
)

data class NutritionTotals(
    val calories: Int,
    val proteinG: Int
)

data class NutritionDay(
    val date: java.time.LocalDate,
    val totals: NutritionTotals,
    val goals: NutritionGoals,
    val entries: List<NutritionEntry>
)

data class NewNutritionEntry(
    val title: String,
    val calories: Int,
    val proteinG: Int,
    val eatenAt: Instant?
) {
    init {
        require(title.isNotBlank()) { "title must not be blank" }
        require(calories >= 0) { "calories must be >= 0" }
        require(proteinG >= 0) { "proteinG must be >= 0" }
    }
}
