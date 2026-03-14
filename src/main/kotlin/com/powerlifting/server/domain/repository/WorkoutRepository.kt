package com.powerlifting.server.domain.repository

import com.powerlifting.server.domain.model.WorkoutHistoryPage
import com.powerlifting.server.domain.model.WorkoutSessionDetail
import com.powerlifting.server.domain.model.WorkoutSet
import java.time.Instant
import java.util.UUID

interface WorkoutRepository {
    suspend fun startSession(
        userId: UUID,
        programWorkoutId: UUID?,
        sleepHours: Double?,
        wellbeing: Int?,
        fatigue: Int?,
        soreness: Int?,
        recommendation: String?
    ): UUID

    suspend fun replaceSets(userId: UUID, sessionId: UUID, sets: List<WorkoutSet>)

    suspend fun finishSession(
        userId: UUID,
        sessionId: UUID,
        durationSec: Int,
        wellbeingRating: Int?
    ): UUID?

    suspend fun getSessionDetail(userId: UUID, sessionId: UUID): WorkoutSessionDetail?

    /**
     * Returns finished sessions older than [before] (or the latest if `before == null`),
     * ordered by `started_at` DESC, capped at [limit] items.
     */
    suspend fun getHistory(userId: UUID, before: Instant?, limit: Int): WorkoutHistoryPage

    suspend fun deleteSession(userId: UUID, sessionId: UUID): Boolean
}
