package com.replus.api.common.security

import com.replus.api.auth.domain.repository.UserRepository
import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component

@Component
class BearerAuthSupport(
    private val devSessionStore: DevSessionStore,
    private val userRepository: UserRepository,
) {
    fun requireUser(authorizationHeader: String?): AuthenticatedUser {
        val token = authorizationHeader
            ?.takeIf { it.startsWith(BEARER_PREFIX, ignoreCase = true) }
            ?.substring(BEARER_PREFIX.length)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: throw CoreException(ErrorType.UNAUTHENTICATED)

        val userId = devSessionStore.resolve(token)
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
