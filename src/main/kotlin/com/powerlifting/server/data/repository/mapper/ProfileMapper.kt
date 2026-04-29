package com.powerlifting.server.data.repository.mapper

import com.powerlifting.server.db.tables.NutritionGoalsTable
import com.powerlifting.server.db.tables.UserProfileTable
import com.powerlifting.server.domain.model.NutritionGoals
import com.powerlifting.server.domain.model.UserProfile
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toUserProfile() = UserProfile(
    heightCm = this[UserProfileTable.heightCm],
    weightKg = this[UserProfileTable.weightKg]?.toDouble(),
    bench1rm = this[UserProfileTable.bench1rm]?.toDouble(),
    squat1rm = this[UserProfileTable.squat1rm]?.toDouble(),
    deadlift1rm = this[UserProfileTable.deadlift1rm]?.toDouble()
)

fun ResultRow.toNutritionGoals() = NutritionGoals(
    caloriesGoal = this[NutritionGoalsTable.caloriesGoal],
    proteinGoalG = this[NutritionGoalsTable.proteinGoalG]
)
