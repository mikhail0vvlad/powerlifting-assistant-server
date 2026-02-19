package com.powerlifting.server.domain.usecase.program

import com.powerlifting.server.domain.model.GenerateProgramSpec
import com.powerlifting.server.domain.model.NewProgramExercise
import com.powerlifting.server.domain.model.ProgramSchedule
import com.powerlifting.server.domain.model.TrainingProgram
import com.powerlifting.server.domain.repository.ProfileRepository
import com.powerlifting.server.domain.repository.ProgramRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

/**
 * Powerlifting periodization with three workout templates (A/B/C) cycled across
 * the user-picked schedule. The schedule decides *when* each workout falls;
 * the cycle decides *which* template each workout uses.
 *
 * - schedule = Weekdays(days, weeks) -> totalWorkouts = days.size * weeks, on those weekdays
 * - schedule = Dates(list)           -> one workout per date, weeks ignored
 * - schedule = null                  -> legacy Mon/Wed/Fri pattern starting at startDate
 */
class GenerateProgramUseCase(
    private val profileRepository: ProfileRepository,
    private val programRepository: ProgramRepository
) {
    suspend operator fun invoke(userId: UUID, spec: GenerateProgramSpec): TrainingProgram {
        val profile = profileRepository.getProfile(userId)
        require(profile.bench1rm != null && profile.bench1rm > 0) { "bench1rm must be set in profile" }
        require(profile.squat1rm != null && profile.squat1rm > 0) { "squat1rm must be set in profile" }
        require(profile.deadlift1rm != null && profile.deadlift1rm > 0) { "deadlift1rm must be set in profile" }

        val today = LocalDate.now(ZoneOffset.UTC)
        val startDate = spec.startDate ?: today
        val weeks = spec.weeks?.coerceIn(MIN_WEEKS, MAX_WEEKS) ?: DEFAULT_WEEKS
        val schedule = spec.schedule

        val dates: List<LocalDate> = when (schedule) {
            is ProgramSchedule.Dates ->
                schedule.dates.filter { !it.isBefore(startDate) }.sorted()
            is ProgramSchedule.Weekdays ->
                buildWeekdayDates(startDate, schedule.days, weeks)
            null ->
                buildWeekdayDates(startDate, defaultWeekdays, weeks)
        }
        require(dates.isNotEmpty()) { "schedule produced no training dates" }

        val name = "Программа пауэрлифтера (${dates.size} тренировок)"
        val templateCode = "PL_3D_${weeks}W"

        programRepository.deactivatePrograms(userId)

        val programId = programRepository.createProgram(
            userId = userId,
            name = name,
            templateCode = templateCode,
            startDate = dates.first(),
            weeks = weeks,
            scheduleJson = schedule?.encode()
        )

        dates.forEachIndexed { idx, date ->
            val template = idx % 3 // 0:A, 1:B, 2:C
            val weekIndex = (idx / 3).coerceAtMost(WEEKLY_PROGRESSION_LAST_INDEX)
            createWorkout(programId, date, template, weekIndex)
        }

        return TrainingProgram(
            id = programId,
            name = name,
            templateCode = templateCode,
            startDate = dates.first(),
            weeks = weeks,
            isActive = true,
            schedule = schedule
        )
    }

    private suspend fun createWorkout(programId: UUID, date: LocalDate, template: Int, wi: Int) {
        when (template) {
            0 -> {
                val id = programRepository.createProgramWorkout(programId, date, "День A: Присед + Жим (тяжёлый)")
                createPyramid(id, "Присед", "squat", weeklySquatPyramid[wi], 1)
                createPyramid(id, "Жим лёжа", "bench", weeklyBenchPyramid[wi], 10)
                createAccessory(id, 20, dayAAccessories)
            }
            1 -> {
                val id = programRepository.createProgramWorkout(programId, date, "День B: Тяга + Жим (объём)")
                createPyramid(id, "Становая тяга", "deadlift", weeklyDeadliftPyramid[wi], 1)
                createPyramid(id, "Жим лёжа", "bench", weeklyBenchMedium[wi], 10)
                createAccessory(id, 20, dayBAccessories)
            }
            else -> {
                val id = programRepository.createProgramWorkout(programId, date, "День C: Присед + Жим (средний)")
                createPyramid(id, "Присед", "squat", weeklySquatMedium[wi], 1)
                createPyramid(id, "Жим лёжа", "bench", weeklyBenchMedium[wi], 10)
                createAccessory(id, 20, dayCAccessories)
            }
        }
    }

    private fun buildWeekdayDates(start: LocalDate, days: Set<DayOfWeek>, weeks: Int): List<LocalDate> {
        if (days.isEmpty()) return emptyList()
        val result = mutableListOf<LocalDate>()
        var weekStart = start
        repeat(weeks) {
            // Walk Monday..Sunday relative to weekStart
            for (offset in 0..6) {
                val d = weekStart.plusDays(offset.toLong())
                if (d.dayOfWeek in days && !d.isBefore(start)) result += d
            }
            weekStart = weekStart.plusWeeks(1)
        }
        // First week may have started mid-week — re-sort to be safe.
        return result.sorted().distinct()
    }

    private suspend fun createPyramid(workoutId: UUID, name: String, lift: String, sets: List<SetGroup>, startOrder: Int) {
        sets.forEachIndexed { idx, sg ->
            programRepository.createExercise(
                programWorkoutId = workoutId,
                exercise = NewProgramExercise(
                    exerciseName = name,
                    orderIndex = startOrder + idx,
                    sets = sg.sets,
                    reps = sg.reps.toString(),
                    percent1rm = sg.percent,
                    liftType = lift
                )
            )
        }
    }

    private suspend fun createAccessory(workoutId: UUID, startOrder: Int, list: List<AccessoryEx>) {
        list.forEachIndexed { idx, acc ->
            programRepository.createExercise(
                programWorkoutId = workoutId,
                exercise = NewProgramExercise(
                    exerciseName = acc.name,
                    orderIndex = startOrder + idx,
                    sets = acc.sets,
                    reps = acc.reps,
                    percent1rm = null,
                    liftType = "other"
                )
            )
        }
    }

    private data class SetGroup(val percent: Double, val reps: Int, val sets: Int)
    private data class AccessoryEx(val name: String, val sets: Int, val reps: String)

    companion object {
        private const val DEFAULT_WEEKS = 4
        private const val MIN_WEEKS = 1
        private const val MAX_WEEKS = 12
        private const val WEEKLY_PROGRESSION_LAST_INDEX = 3

        private val defaultWeekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)

        private val weeklySquatPyramid = listOf(
            listOf(SetGroup(0.55, 4, 1), SetGroup(0.65, 3, 3), SetGroup(0.75, 2, 3)),
            listOf(SetGroup(0.57, 4, 1), SetGroup(0.67, 3, 3), SetGroup(0.77, 2, 3)),
            listOf(SetGroup(0.60, 3, 1), SetGroup(0.70, 3, 3), SetGroup(0.80, 2, 2)),
            listOf(SetGroup(0.60, 3, 1), SetGroup(0.72, 3, 2), SetGroup(0.82, 1, 3))
        )
        private val weeklyBenchPyramid = listOf(
            listOf(SetGroup(0.55, 4, 1), SetGroup(0.65, 3, 3), SetGroup(0.75, 2, 2)),
            listOf(SetGroup(0.57, 4, 1), SetGroup(0.67, 3, 3), SetGroup(0.77, 2, 2)),
            listOf(SetGroup(0.60, 3, 1), SetGroup(0.70, 3, 2), SetGroup(0.80, 1, 3)),
            listOf(SetGroup(0.60, 3, 1), SetGroup(0.72, 2, 2), SetGroup(0.85, 1, 3))
        )
        private val weeklyDeadliftPyramid = listOf(
            listOf(SetGroup(0.55, 3, 1), SetGroup(0.65, 3, 2), SetGroup(0.75, 2, 2)),
            listOf(SetGroup(0.57, 3, 1), SetGroup(0.67, 3, 2), SetGroup(0.77, 2, 2)),
            listOf(SetGroup(0.60, 3, 1), SetGroup(0.70, 2, 2), SetGroup(0.80, 1, 3)),
            listOf(SetGroup(0.60, 3, 1), SetGroup(0.72, 2, 2), SetGroup(0.85, 1, 2))
        )
        private val weeklySquatMedium = listOf(
            listOf(SetGroup(0.50, 5, 1), SetGroup(0.60, 4, 4)),
            listOf(SetGroup(0.52, 5, 1), SetGroup(0.62, 4, 4)),
            listOf(SetGroup(0.55, 4, 1), SetGroup(0.65, 4, 3)),
            listOf(SetGroup(0.55, 4, 1), SetGroup(0.67, 3, 3))
        )
        private val weeklyBenchMedium = listOf(
            listOf(SetGroup(0.50, 5, 1), SetGroup(0.60, 4, 4)),
            listOf(SetGroup(0.52, 5, 1), SetGroup(0.62, 4, 4)),
            listOf(SetGroup(0.55, 4, 1), SetGroup(0.65, 3, 4)),
            listOf(SetGroup(0.55, 4, 1), SetGroup(0.67, 3, 3))
        )
        private val dayAAccessories = listOf(
            AccessoryEx("Тяга штанги в наклоне", 4, "8-10"),
            AccessoryEx("Пресс", 3, "12-15")
        )
        private val dayBAccessories = listOf(
            AccessoryEx("Наклоны со штангой", 4, "8-10"),
            AccessoryEx("Жим стоя", 3, "8-10")
        )
        private val dayCAccessories = listOf(
            AccessoryEx("Жим гантелей", 3, "10-12"),
            AccessoryEx("Французский жим", 3, "10-12"),
            AccessoryEx("Подъём на бицепс", 3, "10-12")
        )
    }
}
