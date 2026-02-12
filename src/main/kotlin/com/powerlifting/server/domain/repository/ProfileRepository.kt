package com.powerlifting.server.domain.repository

import com.powerlifting.server.domain.model.NutritionGoals
import com.powerlifting.server.domain.model.ProfileUpdate
import com.powerlifting.server.domain.model.UserProfile
import com.powerlifting.server.domain.model.UserStats
import java.time.LocalDate
import java.util.UUID

interface ProfileRepository {
    suspend fun getProfile(userId: UUID): UserProfile
    suspend fun getNutritionGoals(userId: UUID): NutritionGoals
    suspend fun updateProfile(userId: UUID, update: ProfileUpdate): UserProfile
    suspend fun updateNutritionGoals(userId: UUID, goals: NutritionGoals): NutritionGoals
    suspend fun getStats(userId: UUID, date: LocalDate): UserStats
}
