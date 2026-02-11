package com.powerlifting.server.domain.model

import java.util.UUID

data class User(
    val id: UUID,
    val firebaseUid: String,
    val email: String?,
    val displayName: String?
)

data class UserProfile(
    val heightCm: Int? = null,
    val weightKg: Double? = null,
    val bench1rm: Double? = null,
    val squat1rm: Double? = null,
    val deadlift1rm: Double? = null
) {
    val hasAllMaxes: Boolean
        get() = bench1rm != null && bench1rm > 0 &&
            squat1rm != null && squat1rm > 0 &&
            deadlift1rm != null && deadlift1rm > 0
}

data class NutritionGoals(
    val caloriesGoal: Int,
    val proteinGoalG: Int
) {
    companion object {
        val DEFAULT = NutritionGoals(caloriesGoal = 2500, proteinGoalG = 150)
    }
}

data class UserStats(
    val achievementsCount: Int,
    val caloriesToday: Int,
    val proteinToday: Int
)

data class ProfileSummary(
    val user: User,
    val profile: UserProfile,
    val nutritionGoals: NutritionGoals,
    val stats: UserStats
)

data class ProfileUpdate(
    val heightCm: Int? = null,
    val weightKg: Double? = null,
    val bench1rm: Double? = null,
    val squat1rm: Double? = null,
    val deadlift1rm: Double? = null
)
