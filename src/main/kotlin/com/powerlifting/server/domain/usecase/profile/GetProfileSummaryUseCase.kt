package com.powerlifting.server.domain.usecase.profile

import com.powerlifting.server.domain.model.ProfileSummary
import com.powerlifting.server.domain.model.User
import com.powerlifting.server.domain.repository.ProfileRepository
import java.time.LocalDate
import java.time.ZoneOffset

class GetProfileSummaryUseCase(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(user: User): ProfileSummary {
        val profile = profileRepository.getProfile(user.id)
        val goals = profileRepository.getNutritionGoals(user.id)
        val stats = profileRepository.getStats(user.id, LocalDate.now(ZoneOffset.UTC))
        return ProfileSummary(user, profile, goals, stats)
    }
}
