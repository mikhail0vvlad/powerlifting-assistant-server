package com.powerlifting.server.domain.repository

import com.powerlifting.server.domain.model.CalendarDay
import com.powerlifting.server.domain.model.NewProgramExercise
import com.powerlifting.server.domain.model.ProgramWorkout
import com.powerlifting.server.domain.model.TrainingProgram
import com.powerlifting.server.domain.model.WorkoutStatus
import java.time.LocalDate
import java.util.UUID

interface ProgramRepository {
    suspend fun deactivatePrograms(userId: UUID)

    suspend fun createProgram(
        userId: UUID,
        name: String,
        templateCode: String,
        startDate: LocalDate,
        weeks: Int,
        scheduleJson: String? = null
    ): UUID

    suspend fun createProgramWorkout(
        programId: UUID,
        date: LocalDate,
        title: String,
        status: String = WorkoutStatus.PLANNED.wire,
        originalWorkoutId: UUID? = null
    ): UUID

    suspend fun createExercise(programWorkoutId: UUID, exercise: NewProgramExercise): UUID

    suspend fun getActiveProgram(userId: UUID): TrainingProgram?

    suspend fun getUpcomingWorkouts(programId: UUID, from: LocalDate, limit: Int = 7): List<ProgramWorkout>

    suspend fun getCalendar(programId: UUID, from: LocalDate, to: LocalDate): List<CalendarDay>

    suspend fun findProgramWorkout(programId: UUID, workoutId: UUID): ProgramWorkout?

    /**
     * Looks up a workout owned by [userId] without requiring the caller to know the
     * containing program. Returns the workout (with exercises) and its programId.
     */
    suspend fun findWorkoutForUser(userId: UUID, workoutId: UUID): Pair<UUID, ProgramWorkout>?

    suspend fun markWorkoutCompleted(programWorkoutId: UUID)

    suspend fun setWorkoutStatus(programWorkoutId: UUID, status: WorkoutStatus)

    /**
     * Returns the number of rows that transitioned from `planned` to `missed`.
     * Used by reads to opportunistically promote stale planned days.
     */
    suspend fun markPastPlannedAsMissed(programId: UUID, before: LocalDate): Int
}
