package com.powerlifting.server.domain.usecase.program

import com.powerlifting.server.domain.model.ActiveProgram
import com.powerlifting.server.domain.repository.ProgramRepository
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class GetActiveProgramUseCase(
    private val programRepository: ProgramRepository
) {
    suspend operator fun invoke(userId: UUID, upcomingLimit: Int = 10): ActiveProgram {
        val active = programRepository.getActiveProgram(userId)
            ?: return ActiveProgram(program = null, upcomingWorkouts = emptyList())

        val today = LocalDate.now(ZoneOffset.UTC)
        // Sweep stale planned days into MISSED so the UI shows them honestly
        // and the user can still reschedule them.
        programRepository.markPastPlannedAsMissed(active.id, today)

        val upcoming = programRepository.getUpcomingWorkouts(active.id, from = today, limit = upcomingLimit)
        return ActiveProgram(program = active, upcomingWorkouts = upcoming)
    }
}
