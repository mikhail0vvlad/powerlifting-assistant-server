package com.powerlifting.server.data.repository.cached

import com.powerlifting.server.data.cache.Cache
import com.powerlifting.server.domain.model.User
import com.powerlifting.server.domain.repository.UserRepository

/**
 * Caches Firebase-uid → User lookups, which happen on every authenticated request.
 *
 * Trade-off: when this returns a cached hit we skip the UPDATE that the underlying
 * impl runs to keep email/displayName in sync with Firebase. That UPDATE is
 * effectively wasteful when nothing changed, and stale data tolerance is high
 * (a 1-hour delay until the TTL expires is acceptable for display fields).
 */
class CachedUserRepository(
    private val delegate: UserRepository,
    private val cache: Cache<String, User>
) : UserRepository {

    override suspend fun getOrCreate(firebaseUid: String, email: String?, displayName: String?): User {
        cache.get(firebaseUid)?.let { return it }
        val user = delegate.getOrCreate(firebaseUid, email, displayName)
        cache.put(firebaseUid, user)
        return user
    }
}
