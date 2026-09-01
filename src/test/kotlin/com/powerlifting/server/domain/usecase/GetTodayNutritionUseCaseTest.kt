package com.powerlifting.server.domain.usecase

import com.powerlifting.server.domain.model.NutritionEntry
import com.powerlifting.server.domain.usecase.nutrition.GetTodayNutritionUseCase
import com.powerlifting.server.fakes.FakeNutritionRepository
import com.powerlifting.server.fakes.FakeProfileRepository
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetTodayNutritionUseCaseTest {

    private val userId = UUID.randomUUID()
    private val date = LocalDate.of(2026, 3, 1)

    @Test
    fun `totals are the sum of entries and goals come from profile`() = runTest {
        val entries = listOf(
            NutritionEntry(
                id = UUID.randomUUID(), title = "Овсянка",
                eatenAt = Instant.parse("2026-03-01T08:00:00Z"), calories = 400, proteinG = 15
            ),
            NutritionEntry(
                id = UUID.randomUUID(), title = "Курица",
                eatenAt = Instant.parse("2026-03-01T14:00:00Z"), calories = 600, proteinG = 55
            )
        )
        val useCase = GetTodayNutritionUseCase(FakeNutritionRepository(entries), FakeProfileRepository())

        val day = useCase(userId, date)

        assertEquals(1000, day.totals.calories)
        assertEquals(70, day.totals.proteinG)
        assertEquals(3000, day.goals.caloriesGoal)
        assertEquals(date, day.date)
    }

    @Test
    fun `empty day yields zero totals, not null`() = runTest {
        val useCase = GetTodayNutritionUseCase(FakeNutritionRepository(emptyList()), FakeProfileRepository())

        val day = useCase(userId, date)

        assertEquals(0, day.totals.calories)
        assertEquals(0, day.totals.proteinG)
        assertEquals(0, day.entries.size)
    }
}
