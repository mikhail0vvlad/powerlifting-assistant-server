package com.powerlifting.server.domain.usecase.workout

import com.powerlifting.server.domain.model.WorkoutHistoryPage
import com.powerlifting.server.domain.repository.WorkoutRepository
import java.time.Instant
import java.util.UUID

class GetWorkoutHistoryUseCase(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(userId: UUID, before: Instant? = null, limit: Int = DEFAULT_LIMIT): WorkoutHistoryPage {
        val cappedLimit = limit.coerceIn(1, MAX_LIMIT)
        return workoutRepository.getHistory(userId, before, cappedLimit)
    }

    companion object {
        const val DEFAULT_LIMIT = 30
        const val MAX_LIMIT = 100
    }
}
