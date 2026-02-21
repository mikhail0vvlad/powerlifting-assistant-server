package com.powerlifting.server.domain.usecase.program

import com.powerlifting.server.domain.model.NewProgramExercise
import com.powerlifting.server.domain.model.ProgramWorkout
import com.powerlifting.server.domain.model.WorkoutStatus
import com.powerlifting.server.domain.repository.ProgramRepository
import java.time.LocalDate
import java.util.UUID

/**
 * Marks the source workout as RESCHEDULED and creates a fresh PLANNED workout
 * on [newDate] carrying the same title and exercises. The new workout's
 * `original_workout_id` points back to the source so the audit trail is intact.
 *
 * The source must belong to [userId] and be in a transferable state
 * (planned or missed). Completed workouts cannot be rescheduled — those have
 * already happened.
 */
class RescheduleWorkoutUseCase(
    private val programRepository: ProgramRepository
) {
    suspend operator fun invoke(userId: UUID, workoutId: UUID, newDate: LocalDate): ProgramWorkout {
        val (programId, source) = programRepository.findWorkoutForUser(userId, workoutId)
            ?: throw IllegalArgumentException("Workout not found")

        val current = WorkoutStatus.parse(source.status)
        require(current == WorkoutStatus.PLANNED || current == WorkoutStatus.MISSED) {
            "Cannot reschedule workout in status '${source.status}'"
        }

        val newId = programRepository.createProgramWorkout(
            programId = programId,
            date = newDate,
            title = source.title,
            status = WorkoutStatus.PLANNED.wire,
            originalWorkoutId = source.id
        )

        source.exercises.forEach { ex ->
            programRepository.createExercise(
                programWorkoutId = newId,
                exercise = NewProgramExercise(
                    exerciseName = ex.exerciseName,
                    orderIndex = ex.orderIndex,
                    sets = ex.sets,
                    reps = ex.reps,
                    percent1rm = ex.percent1rm,
                    liftType = ex.liftType
                )
            )
        }

        programRepository.setWorkoutStatus(source.id, WorkoutStatus.RESCHEDULED)

        return source.copy(id = newId, date = newDate, status = WorkoutStatus.PLANNED.wire, originalWorkoutId = source.id)
    }
}
