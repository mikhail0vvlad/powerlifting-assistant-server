package com.powerlifting.server.domain.usecase.nutrition

import com.powerlifting.server.domain.model.NutritionDay
import com.powerlifting.server.domain.model.NutritionTotals
import com.powerlifting.server.domain.repository.NutritionRepository
import com.powerlifting.server.domain.repository.ProfileRepository
import java.time.LocalDate
import java.util.UUID

class GetTodayNutritionUseCase(
    private val nutritionRepository: NutritionRepository,
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(userId: UUID, date: LocalDate): NutritionDay {
        val entries = nutritionRepository.getEntriesForDate(userId, date)
        val goals = profileRepository.getNutritionGoals(userId)
        val totals = NutritionTotals(
            calories = entries.sumOf { it.calories },
            proteinG = entries.sumOf { it.proteinG }
        )
        return NutritionDay(date = date, totals = totals, goals = goals, entries = entries)
    }
}
