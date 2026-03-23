package com.powerlifting.server.domain.usecase.nutrition

import com.powerlifting.server.domain.model.NewNutritionEntry
import com.powerlifting.server.domain.model.NutritionEntry
import com.powerlifting.server.domain.repository.NutritionRepository
import java.util.UUID

class AddNutritionEntryUseCase(
    private val nutritionRepository: NutritionRepository
) {
    suspend operator fun invoke(userId: UUID, entry: NewNutritionEntry): NutritionEntry =
        nutritionRepository.createEntry(userId, entry)
}
