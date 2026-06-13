package com.replus.api.auth.infrastructure.social

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.jwk.source.JWKSourceBuilder
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType
import java.net.URI
import java.security.interfaces.RSAPublicKey
import java.time.Clock

class NimbusOidcIdTokenVerifier(
    private val clock: Clock,
    private val jwkSourceFactory: (URI) -> JWKSource<SecurityContext> = {
        JWKSourceBuilder.create<SecurityContext>(it.toURL()).build()
    },
) : OidcIdTokenVerifier {
    override fun verify(request: OidcVerificationRequest): OidcIdentityClaims =
        try {
            val signedJwt = SignedJWT.parse(request.idToken)
            if (signedJwt.header.algorithm != JWSAlgorithm.RS256) {
                throw CoreException(ErrorType.UNAUTHENTICATED)
            }
            if (!hasValidSignature(signedJwt, jwkSourceFactory(request.jwksUri))) {
                throw CoreException(ErrorType.UNAUTHENTICATED)
            }

            val claims = signedJwt.jwtClaimsSet
            validateClaims(claims, request)

            OidcIdentityClaims(
                subject = claims.subject,
                email = claims.getStringClaimOrNull("email"),
                displayName = claims.getStringClaimOrNull("name"),
                avatarUrl = claims.getStringClaimOrNull("picture"),
                expiresAt = claims.expirationTime.toInstant(),
            )
        } catch (exception: CoreException) {
            throw exception
        } catch (_: Exception) {
            throw CoreException(ErrorType.UNAUTHENTICATED)
        }

    private fun hasValidSignature(
        signedJwt: SignedJWT,
        jwkSource: JWKSource<SecurityContext>,
    ): Boolean {
        val keys = JWSVerificationKeySelector<SecurityContext>(JWSAlgorithm.RS256, jwkSource)
            .selectJWSKeys(signedJwt.header, null)
        return keys.filterIsInstance<RSAPublicKey>().any { key ->
            runCatching { signedJwt.verify(RSASSAVerifier(key)) }.getOrDefault(false)
        }
    }

    private fun validateClaims(
        claims: JWTClaimsSet,
        request: OidcVerificationRequest,
    ) {
        if (claims.issuer != request.issuer) {
            throw CoreException(ErrorType.UNAUTHENTICATED)
        }
        if (claims.subject?.trim().isNullOrBlank()) {
            throw CoreException(ErrorType.UNAUTHENTICATED)
        }
        val audiences = claims.audience.orEmpty().mapNotNull { it.trim().takeIf(String::isNotBlank) }.toSet()
        if (audiences.intersect(request.acceptedAudiences).isEmpty()) {
            throw CoreException(ErrorType.UNAUTHENTICATED)
        }
        val expiresAt = claims.expirationTime?.toInstant()
            ?: throw CoreException(ErrorType.UNAUTHENTICATED)
        if (!expiresAt.isAfter(clock.instant())) {
            throw CoreException(ErrorType.UNAUTHENTICATED)
        }
    }

    private fun JWTClaimsSet.getStringClaimOrNull(name: String): String? =
        runCatching { getStringClaim(name)?.trim()?.takeIf { it.isNotBlank() } }.getOrNull()
}
