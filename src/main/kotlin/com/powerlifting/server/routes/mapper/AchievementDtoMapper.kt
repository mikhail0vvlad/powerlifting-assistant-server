package com.powerlifting.server.routes.mapper

import com.powerlifting.server.domain.model.Achievement
import com.powerlifting.server.domain.model.NewAchievement
import com.powerlifting.server.dto.AchievementDto
import com.powerlifting.server.dto.CreateAchievementRequest

fun Achievement.toDto() = AchievementDto(
    id = id.toString(),
    createdAtIso = createdAt.toString(),
    note = note,
    photoUrl = photoUrl
)

fun CreateAchievementRequest.toDomain() = NewAchievement(
    note = note,
    photoUrl = photoUrl
)
