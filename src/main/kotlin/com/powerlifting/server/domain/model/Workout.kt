package com.powerlifting.server.domain.model

import java.time.Instant
import java.util.UUID

data class WorkoutSet(
    val exerciseName: String,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val rpe: Double?
) {
    init {
        require(exerciseName.isNotBlank()) { "exerciseName must not be blank" }
        require(setNumber >= 1) { "setNumber must be >= 1" }
        require(weightKg >= 0) { "weightKg must be >= 0" }
        require(reps >= 0) { "reps must be >= 0" }
        rpe?.let { require(it in 0.0..10.0) { "rpe must be 0..10" } }
    }
}

data class RecoveryInputs(
    val sleepHours: Double?,
    val wellbeing: Int?,
    val fatigue: Int?,
    val soreness: Int?
) {
    val isEmpty: Boolean get() =
        sleepHours == null && wellbeing == null && fatigue == null && soreness == null
}

data class StartSessionInput(
    val programWorkoutId: UUID?,
    val recovery: RecoveryInputs
)

data class WorkoutSessionStart(
    val sessionId: UUID,
    val recommendation: String?
)

data class WorkoutSessionDetail(
    val sessionId: UUID,
    val programWorkoutId: UUID?,
    val recommendation: String?,
    val exercises: List<ProgramExercise>,
    val loggedSets: List<WorkoutSet>
)

data class WorkoutHistoryItem(
    val sessionId: UUID,
    val startedAt: Instant,
    val durationSec: Int?,
    val workoutTitle: String?,
    val wellbeingRating: Int?,
    val setsCount: Int
)

/**
 * Cursor-paginated slice of workout history.
 *
 * The cursor is the [Instant] of the last item in [items]; pass it back as
 * `before` on the next call to fetch the next page (sessions are returned in
 * DESC order by `started_at`). When [items] is empty or shorter than the
 * requested page size, [nextCursor] is `null`.
 */
data class WorkoutHistoryPage(
    val items: List<WorkoutHistoryItem>,
    val nextCursor: Instant?
)

data class FinishSessionResult(
    val programWorkoutId: UUID?
)
