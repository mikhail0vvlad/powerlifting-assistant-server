package com.powerlifting.server.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

enum class WorkoutStatus(val wire: String) {
    PLANNED("planned"),
    COMPLETED("completed"),
    MISSED("missed"),
    RESCHEDULED("rescheduled");

    companion object {
        fun parse(s: String?): WorkoutStatus =
            values().firstOrNull { it.wire.equals(s, ignoreCase = true) } ?: PLANNED
    }
}

/**
 * Either a recurring weekly schedule (`weekdays`) or a hand-picked list of dates.
 * Encoded into [TrainingProgramsTable.scheduleJson] as a small string so we
 * don't need a JSON dependency in the data layer.
 */
sealed class ProgramSchedule {
    data class Weekdays(val days: Set<DayOfWeek>) : ProgramSchedule()
    data class Dates(val dates: List<LocalDate>) : ProgramSchedule()

    fun encode(): String = when (this) {
        is Weekdays -> "weekdays:" + days.sortedBy { it.value }.joinToString(",") { it.value.toString() }
        is Dates -> "dates:" + dates.sorted().joinToString(",") { it.toString() }
    }

    companion object {
        fun decode(raw: String?): ProgramSchedule? {
            if (raw.isNullOrBlank()) return null
            val (type, payload) = raw.split(":", limit = 2).let {
                if (it.size != 2) return null else it[0] to it[1]
            }
            return when (type) {
                "weekdays" -> Weekdays(
                    payload.split(",").mapNotNull { it.trim().toIntOrNull() }
                        .filter { it in 1..7 }
                        .map { DayOfWeek.of(it) }
                        .toSet()
                ).takeIf { it.days.isNotEmpty() }
                "dates" -> Dates(
                    payload.split(",").mapNotNull { runCatching { LocalDate.parse(it.trim()) }.getOrNull() }
                ).takeIf { it.dates.isNotEmpty() }
                else -> null
            }
        }
    }
}

data class TrainingProgram(
    val id: UUID,
    val name: String,
    val templateCode: String,
    val startDate: LocalDate,
    val weeks: Int,
    val isActive: Boolean,
    val schedule: ProgramSchedule? = null
)

data class ProgramExercise(
    val id: UUID,
    val exerciseName: String,
    val orderIndex: Int,
    val sets: Int,
    val reps: String,
    val percent1rm: Double?,
    val liftType: String
)

data class ProgramWorkout(
    val id: UUID,
    val date: LocalDate,
    val title: String,
    val status: String,
    val exercises: List<ProgramExercise>,
    val originalWorkoutId: UUID? = null
)

data class ActiveProgram(
    val program: TrainingProgram?,
    val upcomingWorkouts: List<ProgramWorkout>
)

data class CalendarDay(
    val date: LocalDate,
    val title: String,
    val status: String,
    val workoutId: UUID
)

data class TrainingCalendar(
    val from: LocalDate,
    val to: LocalDate,
    val days: List<CalendarDay>
)

data class GenerateProgramSpec(
    val startDate: LocalDate?,
    val weeks: Int?,
    val schedule: ProgramSchedule?
)

data class NewProgramExercise(
    val exerciseName: String,
    val orderIndex: Int,
    val sets: Int,
    val reps: String,
    val percent1rm: Double?,
    val liftType: String
)
