package com.powerlifting.server.domain.usecase.achievements

import com.powerlifting.server.domain.repository.AchievementsRepository
import java.util.UUID

class DeleteAchievementUseCase(
    private val achievementsRepository: AchievementsRepository
) {
    suspend operator fun invoke(userId: UUID, achievementId: UUID): Boolean =
        achievementsRepository.delete(userId, achievementId)
}
