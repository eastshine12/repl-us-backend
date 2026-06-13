package com.replus.api.auth.application

import com.replus.api.auth.domain.model.AuthProvider
import com.replus.api.auth.domain.model.User
import com.replus.api.mission.domain.model.Mission
import com.replus.api.room.domain.model.Room
import com.replus.api.room.domain.model.RoomMember
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class AuthSessionResult(
    val accessToken: String,
    val expiresAt: Instant,
    val user: User,
)

data class SocialLoginCommand(
    val provider: AuthProvider,
    val providerToken: String,
)

data class CurrentUserResult(
    val user: User,
    val rooms: List<RoomSummaryResult>,
)

data class RoomSummaryResult(
    val room: Room,
    val memberCount: Int,
    val currentMember: RoomMember,
    val lastMissionDate: LocalDate?,
    val today: RoomTodaySummaryResult?,
)

data class RoomTodaySummaryResult(
    val mission: Mission,
    val myResponseId: UUID?,
)
