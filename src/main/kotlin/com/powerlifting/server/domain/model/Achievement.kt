package com.powerlifting.server.domain.model

import java.time.Instant
import java.util.UUID

data class Achievement(
    val id: UUID,
    val createdAt: Instant,
    val note: String,
    val photoUrl: String?
)

data class NewAchievement(
    val note: String,
    val photoUrl: String?
) {
    init {
        require(note.isNotBlank()) { "note must not be blank" }
    }
}
