package com.replus.api.auth.infrastructure.social

import com.replus.api.auth.application.port.SocialIdentityVerifier
import com.replus.api.auth.domain.model.AuthProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URI
import java.time.Clock

@Configuration(proxyBeanMethods = false)
class SocialIdentityVerifierConfig {
    @Bean
    @ConditionalOnMissingBean(SocialIdentityVerifier::class)
    fun delegatingSocialIdentityVerifier(
        providerVerifiers: List<ProviderSocialIdentityVerifier>,
    ): SocialIdentityVerifier = DelegatingSocialIdentityVerifier(providerVerifiers)

    @Bean
    @ConditionalOnMissingBean(OidcIdTokenVerifier::class)
    fun oidcIdTokenVerifier(clock: Clock): OidcIdTokenVerifier = NimbusOidcIdTokenVerifier(clock)

    @Bean
    fun googleSocialIdentityVerifier(
        oidcIdTokenVerifier: OidcIdTokenVerifier,
        @Value("\${replus.auth.social.google.client-ids:}") clientIds: String,
        @Value("\${replus.auth.social.google.issuer:https://accounts.google.com}") issuer: String,
        @Value("\${replus.auth.social.google.jwks-uri:https://www.googleapis.com/oauth2/v3/certs}") jwksUri: String,
    ): ProviderSocialIdentityVerifier = OidcSocialIdentityVerifier(
        provider = AuthProvider.GOOGLE,
        issuer = issuer,
        jwksUri = URI.create(jwksUri),
        clientIds = clientIds.toClientIdSet(),
        idTokenVerifier = oidcIdTokenVerifier,
    )

    @Bean
    fun appleSocialIdentityVerifier(
        oidcIdTokenVerifier: OidcIdTokenVerifier,
        @Value("\${replus.auth.social.apple.client-ids:}") clientIds: String,
        @Value("\${replus.auth.social.apple.issuer:https://appleid.apple.com}") issuer: String,
        @Value("\${replus.auth.social.apple.jwks-uri:https://appleid.apple.com/auth/keys}") jwksUri: String,
    ): ProviderSocialIdentityVerifier = OidcSocialIdentityVerifier(
        provider = AuthProvider.APPLE,
        issuer = issuer,
        jwksUri = URI.create(jwksUri),
        clientIds = clientIds.toClientIdSet(),
        idTokenVerifier = oidcIdTokenVerifier,
    )

    private fun String.toClientIdSet(): Set<String> =
        split(",").mapNotNull { it.trim().takeIf(String::isNotBlank) }.toSet()
}
