package com.powerlifting.server.domain.usecase.nutrition

import com.powerlifting.server.domain.repository.NutritionRepository
import java.util.UUID

class DeleteNutritionEntryUseCase(
    private val nutritionRepository: NutritionRepository
) {
    suspend operator fun invoke(userId: UUID, entryId: UUID): Boolean =
        nutritionRepository.deleteEntry(userId, entryId)
}
