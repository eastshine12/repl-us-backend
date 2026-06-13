package com.replus.api.auth.infrastructure.persistence

import com.replus.api.auth.domain.model.AuthProvider
import com.replus.api.auth.domain.model.AuthProviderAccount
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "auth_provider_accounts")
class AuthProviderAccountEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 16)
    var provider: AuthProvider,

    @Column(name = "provider_subject", nullable = false, length = 191)
    var providerSubject: String,

    @Column(name = "email", length = 320)
    var email: String?,

    @Column(name = "display_name", length = 100)
    var displayName: String?,

    @Column(name = "avatar_url")
    var avatarUrl: String?,

    @Column(name = "linked_at", nullable = false)
    var linkedAt: Instant,

    @Column(name = "last_login_at", nullable = false)
    var lastLoginAt: Instant,
) {
    fun toDomain(): AuthProviderAccount =
        AuthProviderAccount(
            id = id,
            userId = userId,
            provider = provider,
            providerSubject = providerSubject,
            email = email,
            displayName = displayName,
            avatarUrl = avatarUrl,
            linkedAt = linkedAt,
            lastLoginAt = lastLoginAt,
        )

    companion object {
        fun from(account: AuthProviderAccount): AuthProviderAccountEntity =
            AuthProviderAccountEntity(
                id = account.id,
                userId = account.userId,
                provider = account.provider,
                providerSubject = account.providerSubject,
                email = account.email,
                displayName = account.displayName,
                avatarUrl = account.avatarUrl,
                linkedAt = account.linkedAt,
                lastLoginAt = account.lastLoginAt,
            )
    }
}
