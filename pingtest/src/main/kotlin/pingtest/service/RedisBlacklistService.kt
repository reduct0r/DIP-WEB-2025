package com.dip.pingtest.service

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class RedisBlacklistService(private val redisTemplate: RedisTemplate<String, String>) {

    companion object {
        const val BLACKLIST_PREFIX = "jwt:blacklist:"
    }

    fun addToBlacklist(token: String, ttl: Long) {
        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "blacklisted", ttl, TimeUnit.MILLISECONDS)
    }

    fun isBlacklisted(token: String): Boolean {
        return redisTemplate.opsForValue().get(BLACKLIST_PREFIX + token) != null
    }
}