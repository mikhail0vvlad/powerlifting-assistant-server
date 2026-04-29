package com.powerlifting.server.domain.usecase.achievements

import com.powerlifting.server.domain.model.Achievement
import com.powerlifting.server.domain.repository.AchievementsRepository
import java.util.UUID

class ListAchievementsUseCase(
    private val achievementsRepository: AchievementsRepository
) {
    suspend operator fun invoke(
        userId: UUID,
        offset: Int = 0,
        limit: Int = DEFAULT_LIMIT
    ): List<Achievement> {
        val cappedLimit = limit.coerceIn(1, MAX_LIMIT)
        val nonNegativeOffset = offset.coerceAtLeast(0)
        return achievementsRepository.list(userId, nonNegativeOffset, cappedLimit)
    }

    companion object {
        const val DEFAULT_LIMIT = 50
        const val MAX_LIMIT = 100
    }
}
