package com.powerlifting.server.data.repository

import com.powerlifting.server.data.repository.mapper.toCalendarDay
import com.powerlifting.server.data.repository.mapper.toProgramExercise
import com.powerlifting.server.data.repository.mapper.toProgramWorkoutWithoutExercises
import com.powerlifting.server.data.repository.mapper.toTrainingProgram
import com.powerlifting.server.db.dbQuery
import com.powerlifting.server.db.tables.ProgramExercisesTable
import com.powerlifting.server.db.tables.ProgramWorkoutsTable
import com.powerlifting.server.db.tables.TrainingProgramsTable
import com.powerlifting.server.domain.model.CalendarDay
import com.powerlifting.server.domain.model.NewProgramExercise
import com.powerlifting.server.domain.model.ProgramWorkout
import com.powerlifting.server.domain.model.TrainingProgram
import com.powerlifting.server.domain.model.WorkoutStatus
import com.powerlifting.server.domain.repository.ProgramRepository
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class ProgramRepositoryImpl : ProgramRepository {

    override suspend fun deactivatePrograms(userId: UUID): Unit = dbQuery {
        TrainingProgramsTable.update({
            (TrainingProgramsTable.userId eq userId) and (TrainingProgramsTable.isActive eq true)
        }) {
            it[isActive] = false
        }
        Unit
    }

    override suspend fun createProgram(
        userId: UUID,
        name: String,
        templateCode: String,
        startDate: LocalDate,
        weeks: Int,
        scheduleJson: String?
    ): UUID = dbQuery {
        TrainingProgramsTable.insertAndGetId {
            it[TrainingProgramsTable.userId] = userId
            it[TrainingProgramsTable.name] = name
            it[TrainingProgramsTable.templateCode] = templateCode
            it[TrainingProgramsTable.startDate] = startDate
            it[TrainingProgramsTable.weeks] = weeks
            it[TrainingProgramsTable.isActive] = true
            it[TrainingProgramsTable.createdAt] = Instant.now()
            it[TrainingProgramsTable.scheduleJson] = scheduleJson
        }.value
    }

    override suspend fun createProgramWorkout(
        programId: UUID,
        date: LocalDate,
        title: String,
        status: String,
        originalWorkoutId: UUID?
    ): UUID = dbQuery {
        ProgramWorkoutsTable.insertAndGetId {
            it[ProgramWorkoutsTable.programId] = programId
            it[workoutDate] = date
            it[ProgramWorkoutsTable.title] = title
            it[ProgramWorkoutsTable.status] = status
            it[ProgramWorkoutsTable.originalWorkoutId] = originalWorkoutId
        }.value
    }

    override suspend fun createExercise(programWorkoutId: UUID, exercise: NewProgramExercise): UUID = dbQuery {
        ProgramExercisesTable.insertAndGetId {
            it[ProgramExercisesTable.programWorkoutId] = programWorkoutId
            it[exerciseName] = exercise.exerciseName
            it[orderIndex] = exercise.orderIndex
            it[sets] = exercise.sets
            it[reps] = exercise.reps
            it[percent1rm] = exercise.percent1rm?.toBigDecimal()
            it[liftType] = exercise.liftType
        }.value
    }

    override suspend fun getActiveProgram(userId: UUID): TrainingProgram? = dbQuery {
        TrainingProgramsTable
            .select { (TrainingProgramsTable.userId eq userId) and (TrainingProgramsTable.isActive eq true) }
            .orderBy(TrainingProgramsTable.createdAt, SortOrder.DESC)
            .limit(1)
            .singleOrNull()
            ?.toTrainingProgram()
    }

    override suspend fun getUpcomingWorkouts(programId: UUID, from: LocalDate, limit: Int): List<ProgramWorkout> = dbQuery {
        val workouts = ProgramWorkoutsTable
            .select {
                (ProgramWorkoutsTable.programId eq programId) and
                    (ProgramWorkoutsTable.workoutDate greaterEq from) and
                    (ProgramWorkoutsTable.status neq WorkoutStatus.RESCHEDULED.wire)
            }
            .orderBy(ProgramWorkoutsTable.workoutDate, SortOrder.ASC)
            .limit(limit)
            .map { it.toProgramWorkoutWithoutExercises() }

        if (workouts.isEmpty()) return@dbQuery emptyList()

        val workoutIds = workouts.map { it.id }
        val exercisesByWorkout = ProgramExercisesTable
            .select { ProgramExercisesTable.programWorkoutId inList workoutIds }
            .orderBy(ProgramExercisesTable.programWorkoutId, SortOrder.ASC)
            .orderBy(ProgramExercisesTable.orderIndex, SortOrder.ASC)
            .groupBy { it[ProgramExercisesTable.programWorkoutId] }
            .mapValues { (_, rows) -> rows.map { it.toProgramExercise() } }

        workouts.map { w -> w.copy(exercises = exercisesByWorkout[w.id].orEmpty()) }
    }

    override suspend fun getCalendar(programId: UUID, from: LocalDate, to: LocalDate): List<CalendarDay> = dbQuery {
        ProgramWorkoutsTable
            .select {
                (ProgramWorkoutsTable.programId eq programId) and
                    (ProgramWorkoutsTable.workoutDate greaterEq from) and
                    (ProgramWorkoutsTable.workoutDate lessEq to)
            }
            .orderBy(ProgramWorkoutsTable.workoutDate, SortOrder.ASC)
            .map { it.toCalendarDay() }
    }

    override suspend fun findProgramWorkout(programId: UUID, workoutId: UUID): ProgramWorkout? = dbQuery {
        val row = ProgramWorkoutsTable
            .select { (ProgramWorkoutsTable.programId eq programId) and (ProgramWorkoutsTable.id eq workoutId) }
            .limit(1)
            .singleOrNull() ?: return@dbQuery null

        val exercises = ProgramExercisesTable
            .select { ProgramExercisesTable.programWorkoutId eq workoutId }
            .orderBy(ProgramExercisesTable.orderIndex, SortOrder.ASC)
            .map { it.toProgramExercise() }

        row.toProgramWorkoutWithoutExercises().copy(exercises = exercises)
    }

    override suspend fun findWorkoutForUser(userId: UUID, workoutId: UUID): Pair<UUID, ProgramWorkout>? = dbQuery {
        val workoutRow = ProgramWorkoutsTable
            .select { ProgramWorkoutsTable.id eq workoutId }
            .limit(1)
            .singleOrNull() ?: return@dbQuery null

        val programId = workoutRow[ProgramWorkoutsTable.programId]

        val ownsProgram = TrainingProgramsTable
            .select { (TrainingProgramsTable.id eq programId) and (TrainingProgramsTable.userId eq userId) }
            .limit(1)
            .any()
        if (!ownsProgram) return@dbQuery null

        val exercises = ProgramExercisesTable
            .select { ProgramExercisesTable.programWorkoutId eq workoutId }
            .orderBy(ProgramExercisesTable.orderIndex, SortOrder.ASC)
            .map { it.toProgramExercise() }

        programId to workoutRow.toProgramWorkoutWithoutExercises().copy(exercises = exercises)
    }

    override suspend fun markWorkoutCompleted(programWorkoutId: UUID): Unit = dbQuery {
        ProgramWorkoutsTable.update({ ProgramWorkoutsTable.id eq programWorkoutId }) {
            it[status] = WorkoutStatus.COMPLETED.wire
        }
        Unit
    }

    override suspend fun setWorkoutStatus(programWorkoutId: UUID, status: WorkoutStatus): Unit = dbQuery {
        ProgramWorkoutsTable.update({ ProgramWorkoutsTable.id eq programWorkoutId }) {
            it[ProgramWorkoutsTable.status] = status.wire
        }
        Unit
    }

    override suspend fun markPastPlannedAsMissed(programId: UUID, before: LocalDate): Int = dbQuery {
        ProgramWorkoutsTable.update({
            (ProgramWorkoutsTable.programId eq programId) and
                (ProgramWorkoutsTable.status eq WorkoutStatus.PLANNED.wire) and
                (ProgramWorkoutsTable.workoutDate less before)
        }) {
            it[status] = WorkoutStatus.MISSED.wire
        }
    }
}
