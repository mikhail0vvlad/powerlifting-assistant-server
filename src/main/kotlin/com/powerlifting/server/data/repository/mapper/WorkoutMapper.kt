package com.powerlifting.server.data.repository.mapper

import com.powerlifting.server.db.tables.WorkoutSetsTable
import com.powerlifting.server.domain.model.WorkoutSet
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toWorkoutSet() = WorkoutSet(
    exerciseName = this[WorkoutSetsTable.exerciseName],
    setNumber = this[WorkoutSetsTable.setNumber],
    weightKg = this[WorkoutSetsTable.weightKg].toDouble(),
    reps = this[WorkoutSetsTable.reps],
    rpe = this[WorkoutSetsTable.rpe]?.toDouble()
)
