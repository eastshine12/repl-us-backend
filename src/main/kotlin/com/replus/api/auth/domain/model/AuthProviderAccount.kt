package com.replus.api.auth.domain.model

import java.time.Instant
import java.util.UUID

data class AuthProviderAccount(
    val id: UUID,
    val userId: UUID,
    val provider: AuthProvider,
    val providerSubject: String,
    val email: String?,
    val displayName: String?,
    val avatarUrl: String?,
    val linkedAt: Instant,
    val lastLoginAt: Instant,
)
