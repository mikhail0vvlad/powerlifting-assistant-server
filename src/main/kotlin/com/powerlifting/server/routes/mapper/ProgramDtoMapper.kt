package com.powerlifting.server.routes.mapper

import com.powerlifting.server.domain.model.ActiveProgram
import com.powerlifting.server.domain.model.CalendarDay
import com.powerlifting.server.domain.model.GenerateProgramSpec
import com.powerlifting.server.domain.model.ProgramExercise
import com.powerlifting.server.domain.model.ProgramSchedule
import com.powerlifting.server.domain.model.ProgramWorkout
import com.powerlifting.server.domain.model.TrainingCalendar
import com.powerlifting.server.domain.model.TrainingProgram
import com.powerlifting.server.dto.ActiveProgramResponse
import com.powerlifting.server.dto.CalendarDayDto
import com.powerlifting.server.dto.CalendarResponse
import com.powerlifting.server.dto.GenerateProgramRequest
import com.powerlifting.server.dto.ProgramExerciseDto
import com.powerlifting.server.dto.ProgramWorkoutDto
import com.powerlifting.server.dto.ScheduleDto
import com.powerlifting.server.dto.TrainingProgramDto
import java.time.DayOfWeek
import java.time.LocalDate

fun ProgramSchedule.toDto(): ScheduleDto = when (this) {
    is ProgramSchedule.Weekdays -> ScheduleDto(
        type = "weekdays",
        weekdays = days.map { it.value }.sorted()
    )
    is ProgramSchedule.Dates -> ScheduleDto(
        type = "dates",
        dates = dates.map { it.toString() }
    )
}

fun ScheduleDto.toDomain(): ProgramSchedule? = when (type.lowercase()) {
    "weekdays" -> {
        val days = weekdays.orEmpty().filter { it in 1..7 }.map { DayOfWeek.of(it) }.toSet()
        if (days.isEmpty()) null else ProgramSchedule.Weekdays(days)
    }
    "dates" -> {
        val parsed = dates.orEmpty().mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (parsed.isEmpty()) null else ProgramSchedule.Dates(parsed)
    }
    else -> null
}

fun TrainingProgram.toDto() = TrainingProgramDto(
    id = id.toString(),
    name = name,
    templateCode = templateCode,
    startDate = startDate.toString(),
    weeks = weeks,
    isActive = isActive,
    schedule = schedule?.toDto()
)

fun ProgramExercise.toDto() = ProgramExerciseDto(
    id = id.toString(),
    exerciseName = exerciseName,
    orderIndex = orderIndex,
    sets = sets,
    reps = reps,
    percent1rm = percent1rm,
    liftType = liftType
)

fun ProgramWorkout.toDto() = ProgramWorkoutDto(
    id = id.toString(),
    date = date.toString(),
    title = title,
    status = status,
    exercises = exercises.map { it.toDto() },
    originalWorkoutId = originalWorkoutId?.toString()
)

fun ActiveProgram.toResponse() = ActiveProgramResponse(
    program = program?.toDto(),
    upcomingWorkouts = upcomingWorkouts.map { it.toDto() }
)

fun CalendarDay.toDto() = CalendarDayDto(
    date = date.toString(),
    title = title,
    status = status,
    workoutId = workoutId.toString()
)

fun TrainingCalendar.toResponse() = CalendarResponse(
    from = from.toString(),
    to = to.toString(),
    days = days.map { it.toDto() }
)

fun emptyCalendarResponse() = CalendarResponse(from = "", to = "", days = emptyList())

fun GenerateProgramRequest.toDomain() = GenerateProgramSpec(
    startDate = startDate?.let { LocalDate.parse(it) },
    weeks = weeks,
    schedule = schedule?.toDomain()
)
