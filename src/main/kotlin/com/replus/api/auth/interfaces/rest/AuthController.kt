package com.replus.api.auth.interfaces.rest

import com.replus.api.auth.application.AuthFacade
import com.replus.api.auth.application.AuthSessionResult
import com.replus.api.auth.application.SocialLoginCommand
import com.replus.api.common.interfaces.rest.dto.RoomTodayResponseStatus
import com.replus.api.common.interfaces.rest.dto.RoomTodaySummaryResponse
import com.replus.api.common.interfaces.rest.dto.toResponse
import com.replus.api.common.interfaces.rest.dto.toSummaryResponse
import com.replus.api.common.security.BearerAuthSupport
import jakarta.validation.Valid
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController(
    private val authFacade: AuthFacade,
    private val bearerAuthSupport: BearerAuthSupport,
) {
    @PostMapping("/api/auth/guest")
    fun createGuestSession(
        @Valid @RequestBody(required = false)
        request: CreateGuestSessionRequest?,
    ): ResponseEntity<AuthSessionResponse> {
        val result = authFacade.createGuestSession(request?.displayName)
        return result.toCreatedSessionResponse()
    }

    @PostMapping("/api/auth/social")
    fun createSocialSession(
        @Valid @RequestBody
        request: SocialLoginRequest,
    ): ResponseEntity<AuthSessionResponse> {
        val result = authFacade.loginWithSocialProvider(
            SocialLoginCommand(
                provider = request.provider,
                providerToken = request.providerToken,
            ),
        )
        return result.toCreatedSessionResponse()
    }

    private fun AuthSessionResult.toCreatedSessionResponse(): ResponseEntity<AuthSessionResponse> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .cacheControl(CacheControl.noStore())
            .body(
                AuthSessionResponse(
                    accessToken = accessToken,
                    expiresAt = expiresAt,
                    user = user.toResponse(),
                ),
            )

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
                    today = it.today?.let { today ->
                        RoomTodaySummaryResponse(
                            missionId = today.mission.id,
                            missionDate = today.mission.missionDate,
                            prompt = today.mission.prompt,
                            category = today.mission.category,
                            myResponseStatus = if (today.myResponseId == null) {
                                RoomTodayResponseStatus.NOT_SUBMITTED
                            } else {
                                RoomTodayResponseStatus.SUBMITTED
                            },
                            myResponseId = today.myResponseId,
                        )
                    },
                )
            },
        )
    }
}
