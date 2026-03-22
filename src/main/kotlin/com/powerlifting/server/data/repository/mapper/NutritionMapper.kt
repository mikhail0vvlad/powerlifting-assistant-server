package com.powerlifting.server.data.repository.mapper

import com.powerlifting.server.db.tables.NutritionEntriesTable
import com.powerlifting.server.domain.model.NutritionEntry
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toNutritionEntry() = NutritionEntry(
    id = this[NutritionEntriesTable.id].value,
    title = this[NutritionEntriesTable.title],
    eatenAt = this[NutritionEntriesTable.eatenAt],
    calories = this[NutritionEntriesTable.calories],
    proteinG = this[NutritionEntriesTable.proteinG]
)
