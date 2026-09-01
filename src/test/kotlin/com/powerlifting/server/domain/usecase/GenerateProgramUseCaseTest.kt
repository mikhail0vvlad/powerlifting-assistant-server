package com.powerlifting.server.domain.usecase

import com.powerlifting.server.domain.model.GenerateProgramSpec
import com.powerlifting.server.domain.model.ProgramSchedule
import com.powerlifting.server.domain.model.UserProfile
import com.powerlifting.server.domain.usecase.program.GenerateProgramUseCase
import com.powerlifting.server.fakes.FakeProfileRepository
import com.powerlifting.server.fakes.FakeProgramRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GenerateProgramUseCaseTest {

    private val userId = UUID.randomUUID()
    // Понедельник.
    private val monday = LocalDate.of(2026, 3, 2)

    private fun useCase(
        profileRepo: FakeProfileRepository = FakeProfileRepository(),
        programRepo: FakeProgramRepository = FakeProgramRepository()
    ) = GenerateProgramUseCase(profileRepo, programRepo) to programRepo

    @Test
    fun `weekday schedule produces days times weeks workouts`() = runTest {
        val (generate, repo) = useCase()

        generate(
            userId,
            GenerateProgramSpec(
                startDate = monday,
                weeks = 4,
                schedule = ProgramSchedule.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
            )
        )

        assertEquals(12, repo.createdWorkouts.size)
        assertTrue(repo.createdWorkouts.all { it.first.dayOfWeek in setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY) })
    }

    @Test
    fun `templates cycle A B C in order`() = runTest {
        val (generate, repo) = useCase()

        generate(
            userId,
            GenerateProgramSpec(monday, 2, ProgramSchedule.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)))
        )

        val titles = repo.createdWorkouts.map { it.second }
        assertTrue(titles[0].startsWith("День A"))
        assertTrue(titles[1].startsWith("День B"))
        assertTrue(titles[2].startsWith("День C"))
        assertTrue(titles[3].startsWith("День A"))
    }

    @Test
    fun `explicit dates before startDate are dropped`() = runTest {
        val (generate, repo) = useCase()

        generate(
            userId,
            GenerateProgramSpec(
                startDate = monday,
                weeks = null,
                schedule = ProgramSchedule.Dates(
                    listOf(monday.minusDays(7), monday, monday.plusDays(2))
                )
            )
        )

        assertEquals(2, repo.createdWorkouts.size)
        assertEquals(monday, repo.createdWorkouts.first().first)
    }

    @Test
    fun `previous programs are deactivated exactly once`() = runTest {
        val (generate, repo) = useCase()

        generate(userId, GenerateProgramSpec(monday, 2, ProgramSchedule.Weekdays(setOf(DayOfWeek.MONDAY))))

        assertEquals(1, repo.deactivateCalls)
    }

    @Test
    fun `missing one-rep max is rejected before anything is written`() = runTest {
        val profileWithoutBench = FakeProfileRepository(
            UserProfile(heightCm = 180, weightKg = 90.0, bench1rm = null, squat1rm = 200.0, deadlift1rm = 250.0)
        )
        val (generate, repo) = useCase(profileRepo = profileWithoutBench)

        assertFailsWith<IllegalArgumentException> {
            generate(userId, GenerateProgramSpec(monday, 4, null))
        }
        assertEquals(0, repo.createdWorkouts.size)
        assertEquals(0, repo.deactivateCalls)
    }

    @Test
    fun `working percentages stay within a sane powerlifting range`() = runTest {
        val (generate, repo) = useCase()

        generate(userId, GenerateProgramSpec(monday, 4, ProgramSchedule.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))))

        val percents = repo.createdExercises.mapNotNull { it.percent1rm }
        assertTrue(percents.isNotEmpty())
        assertTrue(percents.all { it in 0.4..1.0 }, "нашлись проценты вне 40..100%: $percents")
    }
}
