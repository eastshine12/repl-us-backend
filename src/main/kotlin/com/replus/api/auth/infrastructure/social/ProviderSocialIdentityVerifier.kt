package com.replus.api.auth.infrastructure.social

import com.replus.api.auth.application.port.VerifiedSocialIdentity
import com.replus.api.auth.domain.model.AuthProvider

interface ProviderSocialIdentityVerifier {
    val provider: AuthProvider

    fun verify(providerToken: String): VerifiedSocialIdentity
}
