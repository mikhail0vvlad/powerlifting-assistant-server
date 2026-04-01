package com.powerlifting.server.data.cache

/**
 * Minimal cache port. Repositories' caching decorators depend on this interface,
 * not on Caffeine directly, so the cache backend can be swapped (e.g. to Redis)
 * without touching the data layer.
 */
interface Cache<K : Any, V : Any> {
    fun get(key: K): V?
    fun put(key: K, value: V)
    fun invalidate(key: K)
    fun invalidateAll()
}
