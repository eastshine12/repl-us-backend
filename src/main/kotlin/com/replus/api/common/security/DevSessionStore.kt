package com.replus.api.common.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
@ConditionalOnProperty(name = ["replus.auth.session-store"], havingValue = "memory")
class DevSessionStore(
    @Value("\${replus.auth.dev-fixed-tokens-enabled:true}")
    private val fixedDevTokensEnabled: Boolean,
) : SessionStore {
    private val random = SecureRandom()
    private val sessions = ConcurrentHashMap<String, StoredSession>()

    override fun register(userId: UUID, expiresAt: Instant): String {
        val token = "guest_${randomToken()}"
        sessions[token] = StoredSession(userId = userId, expiresAt = expiresAt)
        return token
    }

    override fun resolve(token: String, now: Instant): UUID? =
        sessions[token]
            ?.takeIf { it.expiresAt.isAfter(now) }
            ?.userId
            ?: fixedUserIdFor(token).takeIf { fixedDevTokensEnabled }

    private fun randomToken(): String {
        val bytes = ByteArray(24)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private data class StoredSession(
        val userId: UUID,
        val expiresAt: Instant,
    )

    companion object {
        val MINA_USER_ID: UUID = UUID.fromString("11111111-1111-4111-8111-111111111111")
        val JOON_USER_ID: UUID = UUID.fromString("22222222-2222-4222-8222-222222222222")
        val ARA_USER_ID: UUID = UUID.fromString("33333333-3333-4333-8333-333333333333")

        fun fixedUserIdFor(token: String): UUID? = fixedDevTokens[token]

        private val fixedDevTokens: Map<String, UUID> = mapOf(
            "dev-token-mina" to MINA_USER_ID,
            "dev-token-joon" to JOON_USER_ID,
            "dev-token-ara" to ARA_USER_ID,
        )
    }
}
