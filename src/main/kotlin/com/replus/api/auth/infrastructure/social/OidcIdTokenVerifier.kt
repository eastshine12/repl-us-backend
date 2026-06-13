package com.replus.api.auth.infrastructure.social

import java.net.URI
import java.time.Instant

interface OidcIdTokenVerifier {
    fun verify(request: OidcVerificationRequest): OidcIdentityClaims
}

data class OidcVerificationRequest(
    val idToken: String,
    val issuer: String,
    val jwksUri: URI,
    val acceptedAudiences: Set<String>,
)

data class OidcIdentityClaims(
    val subject: String,
    val email: String?,
    val displayName: String?,
    val avatarUrl: String?,
    val expiresAt: Instant,
)
