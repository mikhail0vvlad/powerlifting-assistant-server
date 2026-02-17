package com.powerlifting.server.domain.repository

import com.powerlifting.server.domain.model.User

interface UserRepository {
    suspend fun getOrCreate(firebaseUid: String, email: String?, displayName: String?): User
}
