package com.powerlifting.server.domain.usecase.workout

import com.powerlifting.server.domain.model.StartSessionInput
import com.powerlifting.server.domain.model.WorkoutSessionStart
import com.powerlifting.server.domain.repository.WorkoutRepository
import com.powerlifting.server.domain.service.RecoveryService
import java.util.UUID

class StartWorkoutSessionUseCase(
    private val workoutRepository: WorkoutRepository,
    private val recoveryService: RecoveryService
) {
    suspend operator fun invoke(userId: UUID, input: StartSessionInput): WorkoutSessionStart {
        val recommendation = recoveryService.makeRecommendation(input.recovery)

        val sessionId = workoutRepository.startSession(
            userId = userId,
            programWorkoutId = input.programWorkoutId,
            sleepHours = input.recovery.sleepHours,
            wellbeing = input.recovery.wellbeing,
            fatigue = input.recovery.fatigue,
            soreness = input.recovery.soreness,
            recommendation = recommendation
        )

        return WorkoutSessionStart(sessionId = sessionId, recommendation = recommendation)
    }
}
