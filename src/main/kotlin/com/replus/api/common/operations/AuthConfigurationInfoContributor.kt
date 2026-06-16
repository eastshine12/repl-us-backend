package com.replus.api.common.operations

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.stereotype.Component

@Component
class AuthConfigurationInfoContributor(
    @Value("\${replus.auth.social.google.client-ids:}") googleClientIds: String,
    @Value("\${replus.auth.social.apple.client-ids:}") appleClientIds: String,
) : InfoContributor {
    private val googleClientIdsConfigured = googleClientIds.hasConfiguredValue()
    private val appleClientIdsConfigured = appleClientIds.hasConfiguredValue()

    override fun contribute(builder: Info.Builder) {
        builder.withDetail(
            "auth",
            mapOf(
                "social" to mapOf(
                    "providers" to mapOf(
                        "google" to providerDetails(googleClientIdsConfigured),
                        "apple" to providerDetails(appleClientIdsConfigured),
                    ),
                ),
            ),
        )
    }

    private fun providerDetails(clientIdsConfigured: Boolean): Map<String, Boolean> =
        mapOf("clientIdsConfigured" to clientIdsConfigured)

    private fun String.hasConfiguredValue(): Boolean =
        split(",").any { it.trim().isNotBlank() }
}
