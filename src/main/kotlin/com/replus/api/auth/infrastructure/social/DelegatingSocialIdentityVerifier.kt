package com.replus.api.auth.infrastructure.social

import com.replus.api.auth.application.SocialLoginCommand
import com.replus.api.auth.application.port.SocialIdentityVerifier
import com.replus.api.auth.application.port.VerifiedSocialIdentity
import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType

class DelegatingSocialIdentityVerifier(
    providerVerifiers: List<ProviderSocialIdentityVerifier>,
) : SocialIdentityVerifier {
    private val providerVerifiers = providerVerifiers.associateBy { it.provider }

    override fun verify(command: SocialLoginCommand): VerifiedSocialIdentity {
        val verifier = providerVerifiers[command.provider]
            ?: throw CoreException(ErrorType.UNAUTHENTICATED)
        return verifier.verify(command.providerToken)
    }
}
