package com.replus.api.auth.infrastructure.persistence

import com.replus.api.common.security.DevSessionStore
import com.replus.api.common.security.SessionStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Instant
import java.util.Base64
import java.util.UUID

@Component
@ConditionalOnProperty(name = ["replus.auth.session-store"], havingValue = "database", matchIfMissing = true)
class JpaSessionStore(
    private val userSessionJpaRepository: UserSessionJpaRepository,
    private val clock: Clock,
    @Value("\${replus.auth.dev-fixed-tokens-enabled:true}")
    private val fixedDevTokensEnabled: Boolean,
) : SessionStore {
    private val random = SecureRandom()

    override fun register(userId: UUID, expiresAt: Instant): String {
        val token = "guest_${randomToken()}"
        userSessionJpaRepository.save(
            UserSessionEntity(
                id = UUID.randomUUID(),
                userId = userId,
                tokenHash = hashToken(token),
                expiresAt = expiresAt,
                createdAt = clock.instant(),
                revokedAt = null,
            ),
        )
        return token
    }

    override fun resolve(token: String, now: Instant): UUID? =
        DevSessionStore.fixedUserIdFor(token)
            .takeIf { fixedDevTokensEnabled }
            ?: userSessionJpaRepository
                .findByTokenHashAndRevokedAtIsNull(hashToken(token))
                ?.takeIf { it.expiresAt.isAfter(now) }
                ?.userId

    private fun randomToken(): String {
        val bytes = ByteArray(24)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
