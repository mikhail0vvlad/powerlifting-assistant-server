package com.powerlifting.server.routes.mapper

import com.powerlifting.server.domain.model.NutritionGoals
import com.powerlifting.server.domain.model.ProfileSummary
import com.powerlifting.server.domain.model.ProfileUpdate
import com.powerlifting.server.domain.model.User
import com.powerlifting.server.domain.model.UserProfile
import com.powerlifting.server.domain.model.UserStats
import com.powerlifting.server.dto.MeResponse
import com.powerlifting.server.dto.NutritionGoalsDto
import com.powerlifting.server.dto.ProfileResponse
import com.powerlifting.server.dto.StatsDto
import com.powerlifting.server.dto.UpdateNutritionGoalsRequest
import com.powerlifting.server.dto.UpdateProfileRequest
import com.powerlifting.server.dto.UserProfileDto

fun User.toMeResponse() = MeResponse(
    userId = id.toString(),
    firebaseUid = firebaseUid,
    email = email,
    displayName = displayName
)

fun UserProfile.toDto() = UserProfileDto(
    heightCm = heightCm,
    weightKg = weightKg,
    bench1rm = bench1rm,
    squat1rm = squat1rm,
    deadlift1rm = deadlift1rm
)

fun NutritionGoals.toDto() = NutritionGoalsDto(
    caloriesGoal = caloriesGoal,
    proteinGoalG = proteinGoalG
)

fun UserStats.toDto() = StatsDto(
    achievementsCount = achievementsCount,
    caloriesToday = caloriesToday,
    proteinToday = proteinToday
)

fun ProfileSummary.toResponse() = ProfileResponse(
    me = user.toMeResponse(),
    profile = profile.toDto(),
    nutritionGoals = nutritionGoals.toDto(),
    stats = stats.toDto()
)

fun UpdateProfileRequest.toDomain() = ProfileUpdate(
    heightCm = heightCm,
    weightKg = weightKg,
    bench1rm = bench1rm,
    squat1rm = squat1rm,
    deadlift1rm = deadlift1rm
)

fun UpdateNutritionGoalsRequest.toDomain() = NutritionGoals(
    caloriesGoal = caloriesGoal,
    proteinGoalG = proteinGoalG
)
