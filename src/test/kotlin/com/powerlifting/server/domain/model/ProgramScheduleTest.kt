package com.powerlifting.server.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProgramScheduleTest {

    @Test
    fun `weekdays survive encode decode roundtrip`() {
        val schedule = ProgramSchedule.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
        assertEquals(schedule, ProgramSchedule.decode(schedule.encode()))
    }

    @Test
    fun `weekdays encode in ascending day order`() {
        val schedule = ProgramSchedule.Weekdays(setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY))
        assertEquals("weekdays:1,5", schedule.encode())
    }

    @Test
    fun `dates survive encode decode roundtrip and come back sorted`() {
        val schedule = ProgramSchedule.Dates(listOf(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 3)))
        val decoded = ProgramSchedule.decode(schedule.encode()) as ProgramSchedule.Dates
        assertEquals(listOf(LocalDate.of(2026, 3, 3), LocalDate.of(2026, 3, 10)), decoded.dates)
    }

    @Test
    fun `garbage decodes to null instead of throwing`() {
        assertNull(ProgramSchedule.decode("weekdays:99,abc"))
        assertNull(ProgramSchedule.decode("нечто:1,2"))
        assertNull(ProgramSchedule.decode(""))
        assertNull(ProgramSchedule.decode(null))
    }
}
