package com.replus.api.auth.infrastructure.social

import com.replus.api.auth.application.port.VerifiedSocialIdentity
import com.replus.api.auth.domain.model.AuthProvider
import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import java.net.URI
import java.time.Instant

class OidcSocialIdentityVerifierTest {
    @Test
    fun `verifies a Google id token with the configured issuer jwks uri and audiences`() {
        val idTokenVerifier = RecordingOidcIdTokenVerifier(
            claims = OidcIdentityClaims(
                subject = "google-123",
                email = "friend@example.test",
                displayName = "Google Friend",
                avatarUrl = "https://cdn.example.test/google.png",
                expiresAt = Instant.parse("2026-06-13T12:00:00Z"),
            ),
        )
        val verifier = OidcSocialIdentityVerifier(
            provider = AuthProvider.GOOGLE,
            issuer = "https://accounts.google.com",
            jwksUri = URI.create("https://www.googleapis.com/oauth2/v3/certs"),
            clientIds = setOf("web-client-id", "ios-client-id"),
            idTokenVerifier = idTokenVerifier,
        )

        val identity = verifier.verify("google-id-token")

        assertThat(idTokenVerifier.request).isEqualTo(
            OidcVerificationRequest(
                idToken = "google-id-token",
                issuer = "https://accounts.google.com",
                jwksUri = URI.create("https://www.googleapis.com/oauth2/v3/certs"),
                acceptedAudiences = setOf("web-client-id", "ios-client-id"),
            ),
        )
        assertThat(identity).isEqualTo(
            VerifiedSocialIdentity(
                provider = AuthProvider.GOOGLE,
                providerSubject = "google-123",
                email = "friend@example.test",
                displayName = "Google Friend",
                avatarUrl = "https://cdn.example.test/google.png",
            ),
        )
    }

    @Test
    fun `rejects id token verification when provider client ids are not configured`() {
        val verifier = OidcSocialIdentityVerifier(
            provider = AuthProvider.APPLE,
            issuer = "https://appleid.apple.com",
            jwksUri = URI.create("https://appleid.apple.com/auth/keys"),
            clientIds = emptySet(),
            idTokenVerifier = RecordingOidcIdTokenVerifier(),
        )

        val thrown = catchThrowable { verifier.verify("apple-id-token") }

        assertThat(thrown).isInstanceOf(CoreException::class.java)
        assertThat((thrown as CoreException).errorType).isEqualTo(ErrorType.UNAUTHENTICATED)
    }

    @Test
    fun `rejects an id token whose verified subject is blank`() {
        val verifier = OidcSocialIdentityVerifier(
            provider = AuthProvider.APPLE,
            issuer = "https://appleid.apple.com",
            jwksUri = URI.create("https://appleid.apple.com/auth/keys"),
            clientIds = setOf("service-id"),
            idTokenVerifier = RecordingOidcIdTokenVerifier(
                claims = OidcIdentityClaims(
                    subject = " ",
                    email = null,
                    displayName = null,
                    avatarUrl = null,
                    expiresAt = Instant.parse("2026-06-13T12:00:00Z"),
                ),
            ),
        )

        val thrown = catchThrowable { verifier.verify("apple-id-token") }

        assertThat(thrown).isInstanceOf(CoreException::class.java)
        assertThat((thrown as CoreException).errorType).isEqualTo(ErrorType.UNAUTHENTICATED)
    }

    private class RecordingOidcIdTokenVerifier(
        private val claims: OidcIdentityClaims = OidcIdentityClaims(
            subject = "subject",
            email = null,
            displayName = null,
            avatarUrl = null,
            expiresAt = Instant.parse("2026-06-13T12:00:00Z"),
        ),
    ) : OidcIdTokenVerifier {
        var request: OidcVerificationRequest? = null
            private set

        override fun verify(request: OidcVerificationRequest): OidcIdentityClaims {
            this.request = request
            return claims
        }
    }
}
