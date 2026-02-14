package com.powerlifting.server.domain.usecase.profile

import com.powerlifting.server.domain.model.ProfileUpdate
import com.powerlifting.server.domain.model.UserProfile
import com.powerlifting.server.domain.repository.ProfileRepository
import java.util.UUID

class UpdateProfileUseCase(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(userId: UUID, update: ProfileUpdate): UserProfile =
        profileRepository.updateProfile(userId, update)
}
