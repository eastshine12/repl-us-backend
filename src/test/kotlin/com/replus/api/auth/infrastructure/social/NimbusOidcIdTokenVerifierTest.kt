package com.replus.api.auth.infrastructure.social

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import java.net.URI
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Date

class NimbusOidcIdTokenVerifierTest {
    private val now = Instant.parse("2026-06-13T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val rsaKey = RSAKeyGenerator(2048)
        .keyID("test-key")
        .generate()

    @Test
    fun `verifies signature issuer audience expiration and maps OIDC claims`() {
        val verifier = verifierWith(rsaKey)
        val idToken = signedToken(
            issuer = "https://accounts.google.com",
            audience = "web-client-id",
            subject = "google-123",
            expiresAt = now.plusSeconds(60),
            claims = mapOf(
                "email" to "friend@example.test",
                "name" to "Google Friend",
                "picture" to "https://cdn.example.test/google.png",
            ),
        )

        val claims = verifier.verify(
            OidcVerificationRequest(
                idToken = idToken,
                issuer = "https://accounts.google.com",
                jwksUri = URI.create("https://www.googleapis.com/oauth2/v3/certs"),
                acceptedAudiences = setOf("web-client-id"),
            ),
        )

        assertThat(claims.subject).isEqualTo("google-123")
        assertThat(claims.email).isEqualTo("friend@example.test")
        assertThat(claims.displayName).isEqualTo("Google Friend")
        assertThat(claims.avatarUrl).isEqualTo("https://cdn.example.test/google.png")
        assertThat(claims.expiresAt).isEqualTo(now.plusSeconds(60))
    }

    @Test
    fun `rejects a token with an audience outside the configured client ids`() {
        val verifier = verifierWith(rsaKey)
        val idToken = signedToken(
            issuer = "https://accounts.google.com",
            audience = "other-client-id",
            subject = "google-123",
            expiresAt = now.plusSeconds(60),
        )

        val thrown = catchThrowable {
            verifier.verify(
                OidcVerificationRequest(
                    idToken = idToken,
                    issuer = "https://accounts.google.com",
                    jwksUri = URI.create("https://www.googleapis.com/oauth2/v3/certs"),
                    acceptedAudiences = setOf("web-client-id"),
                ),
            )
        }

        assertThat(thrown).isInstanceOf(CoreException::class.java)
        assertThat((thrown as CoreException).errorType).isEqualTo(ErrorType.UNAUTHENTICATED)
    }

    @Test
    fun `rejects an expired token`() {
        val verifier = verifierWith(rsaKey)
        val idToken = signedToken(
            issuer = "https://accounts.google.com",
            audience = "web-client-id",
            subject = "google-123",
            expiresAt = now.minusSeconds(1),
        )

        val thrown = catchThrowable {
            verifier.verify(
                OidcVerificationRequest(
                    idToken = idToken,
                    issuer = "https://accounts.google.com",
                    jwksUri = URI.create("https://www.googleapis.com/oauth2/v3/certs"),
                    acceptedAudiences = setOf("web-client-id"),
                ),
            )
        }

        assertThat(thrown).isInstanceOf(CoreException::class.java)
        assertThat((thrown as CoreException).errorType).isEqualTo(ErrorType.UNAUTHENTICATED)
    }

    private fun verifierWith(key: RSAKey): NimbusOidcIdTokenVerifier =
        NimbusOidcIdTokenVerifier(
            clock = clock,
            jwkSourceFactory = { ImmutableJWKSet<SecurityContext>(JWKSet(key.toPublicJWK())) },
        )

    private fun signedToken(
        issuer: String,
        audience: String,
        subject: String,
        expiresAt: Instant,
        claims: Map<String, Any> = emptyMap(),
    ): String {
        val claimsSet = JWTClaimsSet.Builder()
            .issuer(issuer)
            .audience(audience)
            .subject(subject)
            .expirationTime(Date.from(expiresAt))
            .issueTime(Date.from(now))
            .apply { claims.forEach { (key, value) -> claim(key, value) } }
            .build()
        val jwt = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(rsaKey.keyID)
                .type(JOSEObjectType.JWT)
                .build(),
            claimsSet,
        )
        jwt.sign(RSASSASigner(rsaKey))
        return jwt.serialize()
    }
}
