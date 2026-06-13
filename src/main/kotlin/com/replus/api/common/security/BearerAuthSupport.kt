package com.replus.api.common.security

import com.replus.api.auth.domain.repository.UserRepository
import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class BearerAuthSupport(
    private val sessionStore: SessionStore,
    private val userRepository: UserRepository,
    private val clock: Clock,
) {
    fun requireUser(authorizationHeader: String?): AuthenticatedUser {
        val token = authorizationHeader
            ?.takeIf { it.startsWith(BEARER_PREFIX, ignoreCase = true) }
            ?.substring(BEARER_PREFIX.length)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: throw CoreException(ErrorType.UNAUTHENTICATED)

        val userId = sessionStore.resolve(token, clock.instant())
            ?: throw CoreException(ErrorType.UNAUTHENTICATED)

        if (!userRepository.existsById(userId)) {
            throw CoreException(ErrorType.UNAUTHENTICATED)
        }

        return AuthenticatedUser(userId)
    }

    companion object {
        const val AUTHORIZATION_HEADER: String = HttpHeaders.AUTHORIZATION
        private const val BEARER_PREFIX = "Bearer "
    }
}
