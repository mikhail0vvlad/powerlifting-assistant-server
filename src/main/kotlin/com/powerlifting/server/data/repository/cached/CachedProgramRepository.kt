package com.powerlifting.server.data.repository.cached

import com.powerlifting.server.data.cache.Cache
import com.powerlifting.server.domain.model.CalendarDay
import com.powerlifting.server.domain.model.NewProgramExercise
import com.powerlifting.server.domain.model.ProgramWorkout
import com.powerlifting.server.domain.model.TrainingProgram
import com.powerlifting.server.domain.model.WorkoutStatus
import com.powerlifting.server.domain.repository.ProgramRepository
import java.time.LocalDate
import java.util.UUID

class CachedProgramRepository(
    private val delegate: ProgramRepository,
    private val activeProgramCache: Cache<UUID, TrainingProgramHolder>
) : ProgramRepository {

    override suspend fun getActiveProgram(userId: UUID): TrainingProgram? {
        activeProgramCache.get(userId)?.let { return it.program }
        val fresh = delegate.getActiveProgram(userId)
        activeProgramCache.put(userId, TrainingProgramHolder(fresh))
        return fresh
    }

    override suspend fun deactivatePrograms(userId: UUID) {
        delegate.deactivatePrograms(userId)
        activeProgramCache.invalidate(userId)
    }

    override suspend fun createProgram(
        userId: UUID,
        name: String,
        templateCode: String,
        startDate: LocalDate,
        weeks: Int,
        scheduleJson: String?
    ): UUID {
        val id = delegate.createProgram(userId, name, templateCode, startDate, weeks, scheduleJson)
        activeProgramCache.invalidate(userId)
        return id
    }

    override suspend fun createProgramWorkout(
        programId: UUID,
        date: LocalDate,
        title: String,
        status: String,
        originalWorkoutId: UUID?
    ): UUID = delegate.createProgramWorkout(programId, date, title, status, originalWorkoutId)

    override suspend fun createExercise(programWorkoutId: UUID, exercise: NewProgramExercise): UUID =
        delegate.createExercise(programWorkoutId, exercise)

    override suspend fun getUpcomingWorkouts(programId: UUID, from: LocalDate, limit: Int): List<ProgramWorkout> =
        delegate.getUpcomingWorkouts(programId, from, limit)

    override suspend fun getCalendar(programId: UUID, from: LocalDate, to: LocalDate): List<CalendarDay> =
        delegate.getCalendar(programId, from, to)

    override suspend fun findProgramWorkout(programId: UUID, workoutId: UUID): ProgramWorkout? =
        delegate.findProgramWorkout(programId, workoutId)

    override suspend fun findWorkoutForUser(userId: UUID, workoutId: UUID): Pair<UUID, ProgramWorkout>? =
        delegate.findWorkoutForUser(userId, workoutId)

    override suspend fun markWorkoutCompleted(programWorkoutId: UUID) {
        delegate.markWorkoutCompleted(programWorkoutId)
        activeProgramCache.invalidateAll()
    }

    override suspend fun setWorkoutStatus(programWorkoutId: UUID, status: WorkoutStatus) {
        delegate.setWorkoutStatus(programWorkoutId, status)
        activeProgramCache.invalidateAll()
    }

    override suspend fun markPastPlannedAsMissed(programId: UUID, before: LocalDate): Int {
        val updated = delegate.markPastPlannedAsMissed(programId, before)
        if (updated > 0) activeProgramCache.invalidateAll()
        return updated
    }
}

data class TrainingProgramHolder(val program: TrainingProgram?)
