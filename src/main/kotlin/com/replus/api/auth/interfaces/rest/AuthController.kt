package com.replus.api.auth.interfaces.rest

import com.replus.api.auth.application.AuthFacade
import com.replus.api.common.interfaces.rest.dto.toResponse
import com.replus.api.common.interfaces.rest.dto.toSummaryResponse
import com.replus.api.common.security.BearerAuthSupport
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController(
    private val authFacade: AuthFacade,
    private val bearerAuthSupport: BearerAuthSupport,
) {
    @PostMapping("/api/auth/guest")
    @ResponseStatus(HttpStatus.CREATED)
    fun createGuestSession(
        @Valid @RequestBody(required = false)
        request: CreateGuestSessionRequest?,
    ): AuthSessionResponse {
        val result = authFacade.createGuestSession(request?.displayName)
        return AuthSessionResponse(
            accessToken = result.accessToken,
            expiresAt = result.expiresAt,
            user = result.user.toResponse(),
        )
    }

    @GetMapping("/api/me")
    fun getCurrentUser(
        @RequestHeader(BearerAuthSupport.AUTHORIZATION_HEADER, required = false)
        authorization: String?,
    ): CurrentUserResponse {
        val user = bearerAuthSupport.requireUser(authorization)
        val result = authFacade.getCurrentUser(user.userId)
        return CurrentUserResponse(
            user = result.user.toResponse(),
            rooms = result.rooms.map {
                it.room.toSummaryResponse(
                    memberCount = it.memberCount,
                    currentMember = it.currentMember,
                    lastMissionDate = it.lastMissionDate,
                )
            },
        )
    }
}
