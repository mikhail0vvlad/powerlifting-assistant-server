package com.powerlifting.server.routes.mapper

import com.powerlifting.server.domain.model.NewNutritionEntry
import com.powerlifting.server.domain.model.NutritionDay
import com.powerlifting.server.domain.model.NutritionEntry
import com.powerlifting.server.domain.model.NutritionTotals
import com.powerlifting.server.dto.CreateNutritionEntryRequest
import com.powerlifting.server.dto.NutritionEntryDto
import com.powerlifting.server.dto.NutritionTodayResponse
import com.powerlifting.server.dto.NutritionTotalsDto
import java.time.Instant

fun NutritionEntry.toDto() = NutritionEntryDto(
    id = id.toString(),
    title = title,
    eatenAtIso = eatenAt.toString(),
    calories = calories,
    proteinG = proteinG
)

fun NutritionTotals.toDto() = NutritionTotalsDto(
    calories = calories,
    proteinG = proteinG
)

fun NutritionDay.toResponse() = NutritionTodayResponse(
    date = date.toString(),
    totals = totals.toDto(),
    goals = goals.toDto(),
    entries = entries.map { it.toDto() }
)

fun CreateNutritionEntryRequest.toDomain() = NewNutritionEntry(
    title = title,
    calories = calories,
    proteinG = proteinG,
    eatenAt = eatenAtIso?.let { Instant.parse(it) }
)
