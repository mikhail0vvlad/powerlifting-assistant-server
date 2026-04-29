package com.powerlifting.server.data.cache

import com.github.benmanes.caffeine.cache.Caffeine
import java.time.Duration
import com.github.benmanes.caffeine.cache.Cache as CaffeineNative

class CaffeineCache<K : Any, V : Any>(
    maxSize: Long,
    ttl: Duration
) : Cache<K, V> {

    private val backing: CaffeineNative<K, V> = Caffeine.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(ttl)
        .build()

    override fun get(key: K): V? = backing.getIfPresent(key)

    override fun put(key: K, value: V) {
        backing.put(key, value)
    }

    override fun invalidate(key: K) {
        backing.invalidate(key)
    }

    override fun invalidateAll() {
        backing.invalidateAll()
    }
}
