package com.powerlifting.server.domain.usecase.profile

import com.powerlifting.server.domain.model.ProfileUpdate
import com.powerlifting.server.domain.model.UserProfile
import com.powerlifting.server.domain.repository.ProfileRepository
import java.util.UUID

class UpdateProfileUseCase(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(userId: UUID, update: ProfileUpdate): UserProfile {
        // Bounds chosen to fit DB column precision (height int, weight/1RM decimal(5–6, 2))
        // and to reject obviously bogus values (negative weights, 9999 kg bench, etc.).
        update.heightCm?.let { require(it in 50..250) { "heightCm must be 50..250" } }
        update.weightKg?.let { require(it in 20.0..400.0) { "weightKg must be 20..400" } }
        update.bench1rm?.let { require(it in 0.0..1000.0) { "bench1rm must be 0..1000" } }
        update.squat1rm?.let { require(it in 0.0..1000.0) { "squat1rm must be 0..1000" } }
        update.deadlift1rm?.let { require(it in 0.0..1000.0) { "deadlift1rm must be 0..1000" } }
        return profileRepository.updateProfile(userId, update)
    }
}
