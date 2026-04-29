package com.powerlifting.server.domain.usecase.nutrition

import com.powerlifting.server.domain.model.NutritionGoals
import com.powerlifting.server.domain.repository.ProfileRepository
import java.util.UUID

class UpdateNutritionGoalsUseCase(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(userId: UUID, goals: NutritionGoals): NutritionGoals {
        require(goals.caloriesGoal in 0..10_000) { "caloriesGoal out of range" }
        require(goals.proteinGoalG in 0..500) { "proteinGoalG out of range" }
        return profileRepository.updateNutritionGoals(userId, goals)
    }
}
