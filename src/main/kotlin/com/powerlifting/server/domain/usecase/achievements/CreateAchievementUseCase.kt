package com.powerlifting.server.domain.usecase.achievements

import com.powerlifting.server.domain.model.Achievement
import com.powerlifting.server.domain.model.NewAchievement
import com.powerlifting.server.domain.repository.AchievementsRepository
import java.util.UUID

class CreateAchievementUseCase(
    private val achievementsRepository: AchievementsRepository
) {
    suspend operator fun invoke(userId: UUID, achievement: NewAchievement): Achievement =
        achievementsRepository.create(userId, achievement)
}
