package com.powerlifting.server.data.repository.mapper

import com.powerlifting.server.db.tables.AchievementsTable
import com.powerlifting.server.domain.model.Achievement
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toAchievement() = Achievement(
    id = this[AchievementsTable.id].value,
    createdAt = this[AchievementsTable.createdAt],
    note = this[AchievementsTable.note],
    photoUrl = this[AchievementsTable.photoUrl]
)
