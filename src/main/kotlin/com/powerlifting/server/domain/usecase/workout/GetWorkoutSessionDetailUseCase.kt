package com.powerlifting.server.domain.usecase.workout

import com.powerlifting.server.domain.model.WorkoutSessionDetail
import com.powerlifting.server.domain.repository.WorkoutRepository
import java.util.UUID

class GetWorkoutSessionDetailUseCase(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(userId: UUID, sessionId: UUID): WorkoutSessionDetail? =
        workoutRepository.getSessionDetail(userId, sessionId)
}
