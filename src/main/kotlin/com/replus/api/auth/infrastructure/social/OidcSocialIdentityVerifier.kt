package com.replus.api.auth.infrastructure.social

import com.replus.api.auth.application.port.VerifiedSocialIdentity
import com.replus.api.auth.domain.model.AuthProvider
import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType
import java.net.URI

class OidcSocialIdentityVerifier(
    override val provider: AuthProvider,
    private val issuer: String,
    private val jwksUri: URI,
    clientIds: Set<String>,
    private val idTokenVerifier: OidcIdTokenVerifier,
) : ProviderSocialIdentityVerifier {
    private val clientIds = clientIds.mapNotNull { it.trim().takeIf(String::isNotBlank) }.toSet()

    override fun verify(providerToken: String): VerifiedSocialIdentity {
        val token = providerToken.trim().takeIf { it.isNotBlank() }
            ?: throw CoreException(ErrorType.UNAUTHENTICATED)
        if (clientIds.isEmpty()) {
            throw CoreException(ErrorType.UNAUTHENTICATED)
        }

        val claims = idTokenVerifier.verify(
            OidcVerificationRequest(
                idToken = token,
                issuer = issuer,
                jwksUri = jwksUri,
                acceptedAudiences = clientIds,
            ),
        )
        val subject = claims.subject.trim().takeIf { it.isNotBlank() }
            ?: throw CoreException(ErrorType.UNAUTHENTICATED)

        return VerifiedSocialIdentity(
            provider = provider,
            providerSubject = subject,
            email = claims.email?.trim()?.takeIf { it.isNotBlank() },
            displayName = claims.displayName?.trim()?.takeIf { it.isNotBlank() },
            avatarUrl = claims.avatarUrl?.trim()?.takeIf { it.isNotBlank() },
        )
    }
}
