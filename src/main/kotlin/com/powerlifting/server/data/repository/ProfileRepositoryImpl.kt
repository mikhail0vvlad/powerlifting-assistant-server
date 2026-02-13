package com.powerlifting.server.data.repository

import com.powerlifting.server.data.repository.mapper.toNutritionGoals
import com.powerlifting.server.data.repository.mapper.toUserProfile
import com.powerlifting.server.db.dbQuery
import com.powerlifting.server.db.tables.AchievementsTable
import com.powerlifting.server.db.tables.NutritionEntriesTable
import com.powerlifting.server.db.tables.NutritionGoalsTable
import com.powerlifting.server.db.tables.UserProfileTable
import com.powerlifting.server.domain.model.NutritionGoals
import com.powerlifting.server.domain.model.ProfileUpdate
import com.powerlifting.server.domain.model.UserProfile
import com.powerlifting.server.domain.model.UserStats
import com.powerlifting.server.domain.repository.ProfileRepository
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class ProfileRepositoryImpl : ProfileRepository {

    override suspend fun getProfile(userId: UUID): UserProfile = dbQuery {
        UserProfileTable.select { UserProfileTable.id eq userId }
            .limit(1)
            .singleOrNull()
            ?.toUserProfile()
            ?: UserProfile()
    }

    override suspend fun getNutritionGoals(userId: UUID): NutritionGoals = dbQuery {
        NutritionGoalsTable.select { NutritionGoalsTable.id eq userId }
            .limit(1)
            .singleOrNull()
            ?.toNutritionGoals()
            ?: NutritionGoals.DEFAULT
    }

    override suspend fun updateProfile(userId: UUID, update: ProfileUpdate): UserProfile = dbQuery {
        val now = Instant.now()
        val exists = UserProfileTable.select { UserProfileTable.id eq userId }.limit(1).count() > 0
        if (!exists) {
            UserProfileTable.insert {
                it[id] = userId
                it[heightCm] = update.heightCm
                it[weightKg] = update.weightKg?.toBigDecimal()
                it[bench1rm] = update.bench1rm?.toBigDecimal()
                it[squat1rm] = update.squat1rm?.toBigDecimal()
                it[deadlift1rm] = update.deadlift1rm?.toBigDecimal()
                it[updatedAt] = now
            }
        } else {
            UserProfileTable.update({ UserProfileTable.id eq userId }) {
                update.heightCm?.let { v -> it[heightCm] = v }
                update.weightKg?.let { v -> it[weightKg] = v.toBigDecimal() }
                update.bench1rm?.let { v -> it[bench1rm] = v.toBigDecimal() }
                update.squat1rm?.let { v -> it[squat1rm] = v.toBigDecimal() }
                update.deadlift1rm?.let { v -> it[deadlift1rm] = v.toBigDecimal() }
                it[updatedAt] = now
            }
        }

        UserProfileTable.select { UserProfileTable.id eq userId }.single().toUserProfile()
    }

    override suspend fun updateNutritionGoals(userId: UUID, goals: NutritionGoals): NutritionGoals = dbQuery {
        val exists = NutritionGoalsTable.select { NutritionGoalsTable.id eq userId }.limit(1).count() > 0
        if (!exists) {
            NutritionGoalsTable.insert {
                it[id] = userId
                it[caloriesGoal] = goals.caloriesGoal
                it[proteinGoalG] = goals.proteinGoalG
            }
        } else {
            NutritionGoalsTable.update({ NutritionGoalsTable.id eq userId }) {
                it[caloriesGoal] = goals.caloriesGoal
                it[proteinGoalG] = goals.proteinGoalG
            }
        }

        NutritionGoalsTable.select { NutritionGoalsTable.id eq userId }.single().toNutritionGoals()
    }

    override suspend fun getStats(userId: UUID, date: LocalDate): UserStats = dbQuery {
        val achievementsCount = AchievementsTable.select { AchievementsTable.userId eq userId }.count().toInt()

        val start = date.atStartOfDay().toInstant(ZoneOffset.UTC)
        val end = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)

        val caloriesSum = NutritionEntriesTable.calories.sum()
        val proteinSum = NutritionEntriesTable.proteinG.sum()

        val sums = NutritionEntriesTable
            .slice(caloriesSum, proteinSum)
            .select {
                (NutritionEntriesTable.userId eq userId) and
                    (NutritionEntriesTable.eatenAt greaterEq start) and
                    (NutritionEntriesTable.eatenAt less end)
            }
            .single()

        UserStats(
            achievementsCount = achievementsCount,
            caloriesToday = sums[caloriesSum] ?: 0,
            proteinToday = sums[proteinSum] ?: 0
        )
    }
}
