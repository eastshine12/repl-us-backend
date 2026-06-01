package com.replus.api.common.security

import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class DevSessionStore {
    private val random = SecureRandom()
    private val sessions = ConcurrentHashMap<String, UUID>()

    fun register(userId: UUID): String {
        val token = "guest_${randomToken()}"
        sessions[token] = userId
        return token
    }

    fun resolve(token: String): UUID? =
        fixedDevTokens[token] ?: sessions[token]

    private fun randomToken(): String {
        val bytes = ByteArray(24)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    companion object {
        val MINA_USER_ID: UUID = UUID.fromString("11111111-1111-4111-8111-111111111111")
        val JOON_USER_ID: UUID = UUID.fromString("22222222-2222-4222-8222-222222222222")
        val ARA_USER_ID: UUID = UUID.fromString("33333333-3333-4333-8333-333333333333")

        private val fixedDevTokens: Map<String, UUID> = mapOf(
            "dev-token-mina" to MINA_USER_ID,
            "dev-token-joon" to JOON_USER_ID,
            "dev-token-ara" to ARA_USER_ID,
        )
    }
}
