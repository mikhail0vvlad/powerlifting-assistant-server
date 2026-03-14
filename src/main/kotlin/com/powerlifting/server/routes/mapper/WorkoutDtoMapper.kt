package com.powerlifting.server.routes.mapper

import com.powerlifting.server.domain.model.RecoveryInputs
import com.powerlifting.server.domain.model.StartSessionInput
import com.powerlifting.server.domain.model.WorkoutHistoryItem
import com.powerlifting.server.domain.model.WorkoutHistoryPage
import com.powerlifting.server.domain.model.WorkoutSessionDetail
import com.powerlifting.server.domain.model.WorkoutSessionStart
import com.powerlifting.server.domain.model.WorkoutSet
import com.powerlifting.server.dto.StartWorkoutSessionRequest
import com.powerlifting.server.dto.WorkoutHistoryItemDto
import com.powerlifting.server.dto.WorkoutHistoryResponse
import com.powerlifting.server.dto.WorkoutSessionDetailResponse
import com.powerlifting.server.dto.WorkoutSessionResponse
import com.powerlifting.server.dto.WorkoutSetDto
import java.util.UUID

fun WorkoutSetDto.toDomain() = WorkoutSet(
    exerciseName = exerciseName,
    setNumber = setNumber,
    weightKg = weightKg,
    reps = reps,
    rpe = rpe
)

fun WorkoutSet.toDto() = WorkoutSetDto(
    exerciseName = exerciseName,
    setNumber = setNumber,
    weightKg = weightKg,
    reps = reps,
    rpe = rpe
)

fun StartWorkoutSessionRequest.toDomain() = StartSessionInput(
    programWorkoutId = programWorkoutId?.let { UUID.fromString(it) },
    recovery = RecoveryInputs(
        sleepHours = sleepHours,
        wellbeing = wellbeing,
        fatigue = fatigue,
        soreness = soreness
    )
)

fun WorkoutSessionStart.toResponse() = WorkoutSessionResponse(
    sessionId = sessionId.toString(),
    recommendation = recommendation
)

fun WorkoutSessionDetail.toResponse() = WorkoutSessionDetailResponse(
    sessionId = sessionId.toString(),
    programWorkoutId = programWorkoutId?.toString(),
    recommendation = recommendation,
    exercises = exercises.map { it.toDto() },
    loggedSets = loggedSets.map { it.toDto() }
)

fun WorkoutHistoryItem.toDto() = WorkoutHistoryItemDto(
    sessionId = sessionId.toString(),
    date = startedAt.toString(),
    durationSec = durationSec,
    workoutTitle = workoutTitle,
    wellbeingRating = wellbeingRating,
    setsCount = setsCount
)

fun WorkoutHistoryPage.toResponse() = WorkoutHistoryResponse(
    sessions = items.map { it.toDto() },
    nextCursor = nextCursor?.let { encodeCursor(it) }
)

private fun encodeCursor(instant: java.time.Instant): String {
    val raw = instant.toString().toByteArray(Charsets.UTF_8)
    return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(raw)
}

fun decodeCursor(value: String): java.time.Instant {
    val raw = java.util.Base64.getUrlDecoder().decode(value)
    return java.time.Instant.parse(String(raw, Charsets.UTF_8))
}
