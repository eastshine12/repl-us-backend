package com.replus.api.auth.application.port

import com.replus.api.auth.application.SocialLoginCommand
import com.replus.api.auth.domain.model.AuthProvider

interface SocialIdentityVerifier {
    fun verify(command: SocialLoginCommand): VerifiedSocialIdentity
}

data class VerifiedSocialIdentity(
    val provider: AuthProvider,
    val providerSubject: String,
    val email: String?,
    val displayName: String?,
    val avatarUrl: String?,
)
