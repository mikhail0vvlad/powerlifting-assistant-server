package com.powerlifting.server.data.repository.mapper

import com.powerlifting.server.db.tables.ProgramExercisesTable
import com.powerlifting.server.db.tables.ProgramWorkoutsTable
import com.powerlifting.server.db.tables.TrainingProgramsTable
import com.powerlifting.server.domain.model.CalendarDay
import com.powerlifting.server.domain.model.ProgramExercise
import com.powerlifting.server.domain.model.ProgramSchedule
import com.powerlifting.server.domain.model.ProgramWorkout
import com.powerlifting.server.domain.model.TrainingProgram
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toTrainingProgram() = TrainingProgram(
    id = this[TrainingProgramsTable.id].value,
    name = this[TrainingProgramsTable.name],
    templateCode = this[TrainingProgramsTable.templateCode],
    startDate = this[TrainingProgramsTable.startDate],
    weeks = this[TrainingProgramsTable.weeks],
    isActive = this[TrainingProgramsTable.isActive],
    schedule = ProgramSchedule.decode(this[TrainingProgramsTable.scheduleJson])
)

fun ResultRow.toProgramExercise() = ProgramExercise(
    id = this[ProgramExercisesTable.id].value,
    exerciseName = this[ProgramExercisesTable.exerciseName],
    orderIndex = this[ProgramExercisesTable.orderIndex],
    sets = this[ProgramExercisesTable.sets],
    reps = this[ProgramExercisesTable.reps],
    percent1rm = this[ProgramExercisesTable.percent1rm]?.toDouble(),
    liftType = this[ProgramExercisesTable.liftType]
)

fun ResultRow.toProgramWorkoutWithoutExercises() = ProgramWorkout(
    id = this[ProgramWorkoutsTable.id].value,
    date = this[ProgramWorkoutsTable.workoutDate],
    title = this[ProgramWorkoutsTable.title],
    status = this[ProgramWorkoutsTable.status],
    exercises = emptyList(),
    originalWorkoutId = this[ProgramWorkoutsTable.originalWorkoutId]
)

fun ResultRow.toCalendarDay() = CalendarDay(
    date = this[ProgramWorkoutsTable.workoutDate],
    title = this[ProgramWorkoutsTable.title],
    status = this[ProgramWorkoutsTable.status],
    workoutId = this[ProgramWorkoutsTable.id].value
)
