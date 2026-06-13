package com.replus.api.auth.infrastructure.social

import com.replus.api.auth.application.SocialLoginCommand
import com.replus.api.auth.application.port.VerifiedSocialIdentity
import com.replus.api.auth.domain.model.AuthProvider
import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test

class DelegatingSocialIdentityVerifierTest {
    @Test
    fun `delegates verification to the verifier matching the requested provider`() {
        val verifier = DelegatingSocialIdentityVerifier(
            listOf(
                FakeProviderVerifier(AuthProvider.GOOGLE),
                FakeProviderVerifier(AuthProvider.APPLE),
            ),
        )

        val identity = verifier.verify(
            SocialLoginCommand(
                provider = AuthProvider.APPLE,
                providerToken = "apple-id-token",
            ),
        )

        assertThat(identity).isEqualTo(
            VerifiedSocialIdentity(
                provider = AuthProvider.APPLE,
                providerSubject = "APPLE-subject",
                email = "apple@example.test",
                displayName = "APPLE Friend",
                avatarUrl = null,
            ),
        )
    }

    @Test
    fun `rejects a provider without a configured verifier`() {
        val verifier = DelegatingSocialIdentityVerifier(
            listOf(FakeProviderVerifier(AuthProvider.GOOGLE)),
        )

        val thrown = catchThrowable {
            verifier.verify(
                SocialLoginCommand(
                    provider = AuthProvider.APPLE,
                    providerToken = "apple-id-token",
                ),
            )
        }

        assertThat(thrown).isInstanceOf(CoreException::class.java)
        assertThat((thrown as CoreException).errorType).isEqualTo(ErrorType.UNAUTHENTICATED)
    }

    private class FakeProviderVerifier(
        override val provider: AuthProvider,
    ) : ProviderSocialIdentityVerifier {
        override fun verify(providerToken: String): VerifiedSocialIdentity =
            VerifiedSocialIdentity(
                provider = provider,
                providerSubject = "${provider.name}-subject",
                email = "${provider.name.lowercase()}@example.test",
                displayName = "${provider.name} Friend",
                avatarUrl = null,
            )
    }
}
