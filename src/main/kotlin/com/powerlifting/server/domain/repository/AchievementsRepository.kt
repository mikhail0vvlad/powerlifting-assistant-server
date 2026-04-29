package com.powerlifting.server.domain.repository

import com.powerlifting.server.domain.model.Achievement
import com.powerlifting.server.domain.model.NewAchievement
import java.util.UUID

interface AchievementsRepository {
    /**
     * Returns achievements ordered by `created_at DESC`, sliced by [offset]/[limit].
     * Clients can detect the last page by checking `size < limit`.
     */
    suspend fun list(userId: UUID, offset: Int, limit: Int): List<Achievement>

    suspend fun create(userId: UUID, achievement: NewAchievement): Achievement

    suspend fun delete(userId: UUID, achievementId: UUID): Boolean
}
