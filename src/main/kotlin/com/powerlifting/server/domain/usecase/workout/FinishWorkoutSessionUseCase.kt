package com.powerlifting.server.domain.usecase.workout

import com.powerlifting.server.domain.model.FinishSessionResult
import com.powerlifting.server.domain.repository.ProgramRepository
import com.powerlifting.server.domain.repository.WorkoutRepository
import java.util.UUID

class FinishWorkoutSessionUseCase(
    private val workoutRepository: WorkoutRepository,
    private val programRepository: ProgramRepository
) {
    suspend operator fun invoke(
        userId: UUID,
        sessionId: UUID,
        durationSec: Int,
        wellbeingRating: Int?
    ): FinishSessionResult {
        require(durationSec >= 0) { "durationSec must be >= 0" }
        wellbeingRating?.let { require(it in 1..5) { "wellbeingRating must be 1..5" } }

        val programWorkoutId = workoutRepository.finishSession(userId, sessionId, durationSec, wellbeingRating)
        if (programWorkoutId != null) {
            programRepository.markWorkoutCompleted(programWorkoutId)
        }
        return FinishSessionResult(programWorkoutId = programWorkoutId)
    }
}
