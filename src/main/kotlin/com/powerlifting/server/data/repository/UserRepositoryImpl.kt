package com.powerlifting.server.data.repository

import com.powerlifting.server.data.repository.mapper.toUser
import com.powerlifting.server.db.dbQuery
import com.powerlifting.server.db.tables.NutritionGoalsTable
import com.powerlifting.server.db.tables.UserProfileTable
import com.powerlifting.server.db.tables.UsersTable
import com.powerlifting.server.domain.model.NutritionGoals
import com.powerlifting.server.domain.model.User
import com.powerlifting.server.domain.repository.UserRepository
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.time.Instant

class UserRepositoryImpl : UserRepository {

    override suspend fun getOrCreate(firebaseUid: String, email: String?, displayName: String?): User = dbQuery {
        val existing = UsersTable.select { UsersTable.firebaseUid eq firebaseUid }
            .limit(1)
            .singleOrNull()

        if (existing != null) {
            UsersTable.update({ UsersTable.id eq existing[UsersTable.id].value }) {
                it[UsersTable.email] = email
                it[UsersTable.displayName] = displayName
            }
            return@dbQuery User(
                id = existing[UsersTable.id].value,
                firebaseUid = existing[UsersTable.firebaseUid],
                email = email ?: existing[UsersTable.email],
                displayName = displayName ?: existing[UsersTable.displayName]
            )
        }

        val now = Instant.now()
        val newId = UsersTable.insertAndGetId {
            it[UsersTable.firebaseUid] = firebaseUid
            it[UsersTable.email] = email
            it[UsersTable.displayName] = displayName
            it[UsersTable.createdAt] = now
        }.value

        UserProfileTable.insertIgnore {
            it[id] = newId
            it[heightCm] = null
            it[weightKg] = null
            it[bench1rm] = null
            it[squat1rm] = null
            it[deadlift1rm] = null
            it[updatedAt] = now
        }

        NutritionGoalsTable.insertIgnore {
            it[id] = newId
            it[caloriesGoal] = NutritionGoals.DEFAULT.caloriesGoal
            it[proteinGoalG] = NutritionGoals.DEFAULT.proteinGoalG
        }

        User(
            id = newId,
            firebaseUid = firebaseUid,
            email = email,
            displayName = displayName
        )
    }
}
