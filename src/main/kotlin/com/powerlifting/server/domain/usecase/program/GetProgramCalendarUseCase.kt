package com.powerlifting.server.domain.usecase.program

import com.powerlifting.server.domain.model.TrainingCalendar
import com.powerlifting.server.domain.repository.ProgramRepository
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class GetProgramCalendarUseCase(
    private val programRepository: ProgramRepository
) {
    suspend operator fun invoke(userId: UUID, from: LocalDate?, to: LocalDate?): TrainingCalendar? {
        val active = programRepository.getActiveProgram(userId) ?: return null

        val effectiveFrom = from ?: LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1)
        val effectiveTo = to ?: effectiveFrom.plusMonths(1).minusDays(1)

        // Same opportunistic sweep as the active-program path.
        programRepository.markPastPlannedAsMissed(active.id, LocalDate.now(ZoneOffset.UTC))

        val days = programRepository.getCalendar(active.id, effectiveFrom, effectiveTo)
        return TrainingCalendar(from = effectiveFrom, to = effectiveTo, days = days)
    }
}
