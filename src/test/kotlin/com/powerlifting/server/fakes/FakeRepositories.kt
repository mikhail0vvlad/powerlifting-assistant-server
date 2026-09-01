package com.powerlifting.server.fakes

import com.powerlifting.server.domain.model.CalendarDay
import com.powerlifting.server.domain.model.NewNutritionEntry
import com.powerlifting.server.domain.model.NewProgramExercise
import com.powerlifting.server.domain.model.NutritionEntry
import com.powerlifting.server.domain.model.NutritionGoals
import com.powerlifting.server.domain.model.ProfileUpdate
import com.powerlifting.server.domain.model.ProgramWorkout
import com.powerlifting.server.domain.model.TrainingProgram
import com.powerlifting.server.domain.model.UserProfile
import com.powerlifting.server.domain.model.UserStats
import com.powerlifting.server.domain.model.WorkoutStatus
import com.powerlifting.server.domain.repository.NutritionRepository
import com.powerlifting.server.domain.repository.ProfileRepository
import com.powerlifting.server.domain.repository.ProgramRepository
import java.time.LocalDate
import java.util.UUID

class FakeProfileRepository(
    private val profile: UserProfile = UserProfile(
        heightCm = 180, weightKg = 90.0, bench1rm = 100.0, squat1rm = 200.0, deadlift1rm = 250.0
    ),
    private val goals: NutritionGoals = NutritionGoals(caloriesGoal = 3000, proteinGoalG = 180)
) : ProfileRepository {
    override suspend fun getProfile(userId: UUID): UserProfile = profile
    override suspend fun getNutritionGoals(userId: UUID): NutritionGoals = goals
    override suspend fun updateProfile(userId: UUID, update: ProfileUpdate): UserProfile = profile
    override suspend fun updateNutritionGoals(userId: UUID, goals: NutritionGoals): NutritionGoals = goals
    override suspend fun getStats(userId: UUID, date: LocalDate): UserStats =
        throw UnsupportedOperationException("not needed in these tests")
}

class FakeProgramRepository : ProgramRepository {
    var deactivateCalls: Int = 0
        private set
    /** Пары (дата, заголовок) в порядке создания — по ним проверяем чередование A/B/C. */
    val createdWorkouts = mutableListOf<Pair<LocalDate, String>>()
    val createdExercises = mutableListOf<NewProgramExercise>()

    override suspend fun deactivatePrograms(userId: UUID) { deactivateCalls++ }

    override suspend fun createProgram(
        userId: UUID, name: String, templateCode: String,
        startDate: LocalDate, weeks: Int, scheduleJson: String?
    ): UUID = UUID.randomUUID()

    override suspend fun createProgramWorkout(
        programId: UUID, date: LocalDate, title: String, status: String, originalWorkoutId: UUID?
    ): UUID {
        createdWorkouts += date to title
        return UUID.randomUUID()
    }

    override suspend fun createExercise(programWorkoutId: UUID, exercise: NewProgramExercise): UUID {
        createdExercises += exercise
        return UUID.randomUUID()
    }

    override suspend fun getActiveProgram(userId: UUID): TrainingProgram? = null
    override suspend fun getUpcomingWorkouts(programId: UUID, from: LocalDate, limit: Int): List<ProgramWorkout> = emptyList()
    override suspend fun getCalendar(programId: UUID, from: LocalDate, to: LocalDate): List<CalendarDay> = emptyList()
    override suspend fun findProgramWorkout(programId: UUID, workoutId: UUID): ProgramWorkout? = null
    override suspend fun findWorkoutForUser(userId: UUID, workoutId: UUID): Pair<UUID, ProgramWorkout>? = null
    override suspend fun markWorkoutCompleted(programWorkoutId: UUID) = Unit
    override suspend fun setWorkoutStatus(programWorkoutId: UUID, status: WorkoutStatus) = Unit
    override suspend fun markPastPlannedAsMissed(programId: UUID, before: LocalDate): Int = 0
}

class FakeNutritionRepository(private val entries: List<NutritionEntry>) : NutritionRepository {
    override suspend fun getEntriesForDate(userId: UUID, date: LocalDate): List<NutritionEntry> = entries
    override suspend fun createEntry(userId: UUID, entry: NewNutritionEntry): NutritionEntry =
        throw UnsupportedOperationException("not needed in these tests")
    override suspend fun deleteEntry(userId: UUID, entryId: UUID): Boolean =
        throw UnsupportedOperationException("not needed in these tests")
}
