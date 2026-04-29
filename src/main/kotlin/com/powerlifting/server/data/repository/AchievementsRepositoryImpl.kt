package com.powerlifting.server.data.repository

import com.powerlifting.server.data.repository.mapper.toAchievement
import com.powerlifting.server.db.dbQuery
import com.powerlifting.server.db.tables.AchievementsTable
import com.powerlifting.server.domain.model.Achievement
import com.powerlifting.server.domain.model.NewAchievement
import com.powerlifting.server.domain.repository.AchievementsRepository
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import java.time.Instant
import java.util.UUID

class AchievementsRepositoryImpl : AchievementsRepository {

    override suspend fun list(userId: UUID, offset: Int, limit: Int): List<Achievement> = dbQuery {
        AchievementsTable
            .select { AchievementsTable.userId eq userId }
            .orderBy(AchievementsTable.createdAt, SortOrder.DESC)
            .limit(limit, offset = offset.toLong())
            .map { it.toAchievement() }
    }

    override suspend fun create(userId: UUID, achievement: NewAchievement): Achievement = dbQuery {
        val now = Instant.now()
        val id = AchievementsTable.insertAndGetId {
            it[AchievementsTable.userId] = userId
            it[createdAt] = now
            it[note] = achievement.note
            it[photoUrl] = achievement.photoUrl
        }.value

        Achievement(
            id = id,
            createdAt = now,
            note = achievement.note,
            photoUrl = achievement.photoUrl
        )
    }

    override suspend fun delete(userId: UUID, achievementId: UUID): Boolean = dbQuery {
        AchievementsTable.deleteWhere {
            (AchievementsTable.id eq achievementId) and (AchievementsTable.userId eq userId)
        } > 0
    }
}
