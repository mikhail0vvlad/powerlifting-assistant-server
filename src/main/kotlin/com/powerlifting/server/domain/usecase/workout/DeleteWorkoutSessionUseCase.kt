package com.powerlifting.server.domain.usecase.workout

import com.powerlifting.server.domain.repository.WorkoutRepository
import java.util.UUID

class DeleteWorkoutSessionUseCase(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(userId: UUID, sessionId: UUID): Boolean =
        workoutRepository.deleteSession(userId, sessionId)
}
