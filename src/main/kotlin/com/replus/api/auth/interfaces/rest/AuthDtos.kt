package com.replus.api.auth.interfaces.rest

import com.replus.api.common.interfaces.rest.dto.RoomSummaryResponse
import com.replus.api.common.interfaces.rest.dto.UserResponse
import jakarta.validation.constraints.Size
import java.time.Instant

data class CreateGuestSessionRequest(
    @field:Size(min = 1, max = 24)
    val displayName: String? = null,
)

data class AuthSessionResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresAt: Instant,
    val user: UserResponse,
)

data class CurrentUserResponse(
    val user: UserResponse,
    val rooms: List<RoomSummaryResponse>,
)
