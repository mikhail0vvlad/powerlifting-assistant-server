package com.powerlifting.server.domain.usecase.program

import com.powerlifting.server.domain.model.WorkoutStatus
import com.powerlifting.server.domain.repository.ProgramRepository
import java.util.UUID

/**
 * Explicitly marks a workout as MISSED at the user's request. Auto-missed
 * sweeping (in [GetActiveProgramUseCase]/[GetProgramCalendarUseCase]) handles
 * the implicit case where the day passed without action.
 */
class SkipWorkoutUseCase(
    private val programRepository: ProgramRepository
) {
    suspend operator fun invoke(userId: UUID, workoutId: UUID) {
        val (_, source) = programRepository.findWorkoutForUser(userId, workoutId)
            ?: throw IllegalArgumentException("Workout not found")
        require(WorkoutStatus.parse(source.status) == WorkoutStatus.PLANNED) {
            "Only planned workouts can be skipped"
        }
        programRepository.setWorkoutStatus(source.id, WorkoutStatus.MISSED)
    }
}
