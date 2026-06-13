package com.replus.api.auth.infrastructure.social

import com.replus.api.auth.application.SocialLoginCommand
import com.replus.api.auth.application.port.SocialIdentityVerifier
import com.replus.api.auth.application.port.VerifiedSocialIdentity
import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType

class UnsupportedSocialIdentityVerifier : SocialIdentityVerifier {
    override fun verify(command: SocialLoginCommand): VerifiedSocialIdentity {
        throw CoreException(ErrorType.UNAUTHENTICATED)
    }
}
