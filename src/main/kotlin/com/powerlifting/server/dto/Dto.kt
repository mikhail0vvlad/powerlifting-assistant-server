package com.powerlifting.server.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorResponse(
    val error: String,
    val details: String? = null
)

@Serializable
data class MeResponse(
    val userId: String,
    val firebaseUid: String,
    val email: String? = null,
    val displayName: String? = null
)

@Serializable
data class UserProfileDto(
    val heightCm: Int? = null,
    val weightKg: Double? = null,
    val bench1rm: Double? = null,
    val squat1rm: Double? = null,
    val deadlift1rm: Double? = null
)

@Serializable
data class NutritionGoalsDto(
    val caloriesGoal: Int,
    val proteinGoalG: Int
)

@Serializable
data class ProfileResponse(
    val me: MeResponse,
    val profile: UserProfileDto,
    val nutritionGoals: NutritionGoalsDto,
    val stats: StatsDto
)

@Serializable
data class StatsDto(
    val achievementsCount: Int,
    val caloriesToday: Int,
    val proteinToday: Int
)

@Serializable
data class UpdateProfileRequest(
    val heightCm: Int? = null,
    val weightKg: Double? = null,
    val bench1rm: Double? = null,
    val squat1rm: Double? = null,
    val deadlift1rm: Double? = null
) {
    init {
        // Range checks are also enforced server-side in UpdateProfileUseCase; the
        // duplication here gives the same 400 even if the use case is bypassed.
        heightCm?.let { require(it in 50..250) { "heightCm must be 50..250" } }
        weightKg?.let { require(it in 20.0..400.0) { "weightKg must be 20..400" } }
        bench1rm?.let { require(it in 0.0..1000.0) { "bench1rm must be 0..1000" } }
        squat1rm?.let { require(it in 0.0..1000.0) { "squat1rm must be 0..1000" } }
        deadlift1rm?.let { require(it in 0.0..1000.0) { "deadlift1rm must be 0..1000" } }
    }
}

@Serializable
data class UpdateNutritionGoalsRequest(
    val caloriesGoal: Int,
    val proteinGoalG: Int
) {
    init {
        require(caloriesGoal in 100..20_000) { "caloriesGoal must be 100..20000" }
        require(proteinGoalG in 0..2_000) { "proteinGoalG must be 0..2000" }
    }
}

@Serializable
data class NutritionEntryDto(
    val id: String,
    val title: String,
    val eatenAtIso: String,
    val calories: Int,
    val proteinG: Int
)

@Serializable
data class CreateNutritionEntryRequest(
    val title: String,
    val calories: Int,
    val proteinG: Int,
    /** Optional ISO-8601 string. If null, server uses now(). */
    val eatenAtIso: String? = null
) {
    init {
        require(title.length in 1..200) { "title length must be 1..200" }
        require(calories in 0..10_000) { "calories must be 0..10000" }
        require(proteinG in 0..2_000) { "proteinG must be 0..2000" }
    }
}

@Serializable
data class NutritionTodayResponse(
    val date: String,
    val totals: NutritionTotalsDto,
    val goals: NutritionGoalsDto,
    val entries: List<NutritionEntryDto>
)

@Serializable
data class NutritionTotalsDto(
    val calories: Int,
    val proteinG: Int
)

/**
 * Two flavours; only one of `weekdays`/`dates` should be populated.
 * - type=weekdays: weekdays is a list of ISO weekday numbers (1=Mon..7=Sun)
 * - type=dates:    dates is a list of ISO YYYY-MM-DD strings
 */
@Serializable
data class ScheduleDto(
    val type: String,
    val weekdays: List<Int>? = null,
    val dates: List<String>? = null
)

@Serializable
data class GenerateProgramRequest(
    /** ISO date YYYY-MM-DD. If null -> today. */
    val startDate: String? = null,
    val weeks: Int? = null,
    val schedule: ScheduleDto? = null
)

@Serializable
data class RescheduleWorkoutRequest(
    val newDate: String
)

@Serializable
data class TrainingProgramDto(
    val id: String,
    val name: String,
    val templateCode: String,
    val startDate: String,
    val weeks: Int,
    val isActive: Boolean,
    val schedule: ScheduleDto? = null
)

@Serializable
data class CalendarDayDto(
    val date: String,
    val title: String,
    val status: String,
    val workoutId: String
)

@Serializable
data class CalendarResponse(
    val from: String,
    val to: String,
    val days: List<CalendarDayDto>
)

@Serializable
data class ProgramWorkoutDto(
    val id: String,
    val date: String,
    val title: String,
    val status: String,
    val exercises: List<ProgramExerciseDto>,
    val originalWorkoutId: String? = null
)

@Serializable
data class ProgramExerciseDto(
    val id: String,
    val exerciseName: String,
    val orderIndex: Int,
    val sets: Int,
    val reps: String,
    val percent1rm: Double? = null,
    val liftType: String
)

@Serializable
data class ActiveProgramResponse(
    val program: TrainingProgramDto?,
    val upcomingWorkouts: List<ProgramWorkoutDto>
)

@Serializable
data class StartWorkoutSessionRequest(
    val programWorkoutId: String? = null,
    val sleepHours: Double? = null,
    val wellbeing: Int? = null,
    val fatigue: Int? = null,
    val soreness: Int? = null
) {
    init {
        sleepHours?.let { require(it in 0.0..24.0) { "sleepHours must be 0..24" } }
        wellbeing?.let { require(it in 1..10) { "wellbeing must be 1..10" } }
        fatigue?.let { require(it in 1..10) { "fatigue must be 1..10" } }
        soreness?.let { require(it in 1..10) { "soreness must be 1..10" } }
    }
}

@Serializable
data class WorkoutSessionResponse(
    val sessionId: String,
    val recommendation: String? = null
)

@Serializable
data class AddWorkoutSetsRequest(
    val sets: List<WorkoutSetDto>
) {
    init {
        require(sets.size <= 200) { "too many sets in one request" }
    }
}

@Serializable
data class WorkoutSetDto(
    val exerciseName: String,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val rpe: Double? = null
) {
    init {
        require(exerciseName.length in 1..100) { "exerciseName length must be 1..100" }
        require(setNumber in 1..200) { "setNumber must be 1..200" }
        // weightKg must fit decimal(6,2) — max 9999.99
        require(weightKg in 0.0..2_000.0) { "weightKg must be 0..2000" }
        require(reps in 0..1_000) { "reps must be 0..1000" }
        rpe?.let { require(it in 0.0..10.0) { "rpe must be 0..10" } }
    }
}

@Serializable
data class FinishWorkoutSessionRequest(
    val workoutDurationSec: Int
)

@Serializable
data class AchievementDto(
    val id: String,
    val createdAtIso: String,
    val note: String,
    val photoUrl: String? = null
)

@Serializable
data class CreateAchievementRequest(
    val note: String,
    val photoUrl: String? = null
) {
    init {
        require(note.length in 1..2000) { "note length must be 1..2000" }
        photoUrl?.let {
            require(it.length <= 1000) { "photoUrl length must be <= 1000" }
            // Restrict to https only — rules out javascript:, file:, http:, etc.
            // The client renders these via Coil, so an http: or javascript: URL
            // could end up exposed to other surfaces over time.
            require(it.startsWith("https://")) { "photoUrl must be an https URL" }
        }
    }
}

@Serializable
data class FinishWorkoutWithRatingRequest(
    val workoutDurationSec: Int,
    val wellbeingRating: Int? = null
) {
    init {
        // 0..86400 = up to 24h. wellbeingRating 1..5 already enforced in use case.
        require(workoutDurationSec in 0..86_400) { "workoutDurationSec must be 0..86400" }
    }
}

@Serializable
data class WorkoutHistoryItemDto(
    val sessionId: String,
    val date: String,
    val durationSec: Int?,
    val workoutTitle: String?,
    val wellbeingRating: Int?,
    val setsCount: Int
)

@Serializable
data class WorkoutHistoryResponse(
    val sessions: List<WorkoutHistoryItemDto>,
    /**
     * Opaque base64url-encoded cursor for the next page, or null on the last page.
     * Pass it as `?cursor=...` to fetch the next page.
     */
    val nextCursor: String? = null
)

@Serializable
data class WorkoutSessionDetailResponse(
    val sessionId: String,
    val programWorkoutId: String?,
    val recommendation: String?,
    val exercises: List<ProgramExerciseDto>,
    val loggedSets: List<WorkoutSetDto>
)
