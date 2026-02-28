package com.powerlifting.server.domain.usecase.workout

import com.powerlifting.server.domain.model.WorkoutSet
import com.powerlifting.server.domain.repository.WorkoutRepository
import java.util.UUID

class AddWorkoutSetsUseCase(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(userId: UUID, sessionId: UUID, sets: List<WorkoutSet>) {
        workoutRepository.replaceSets(userId, sessionId, sets)
    }
}
