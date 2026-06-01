package com.replus.api.common.interfaces.rest.dto

import com.replus.api.auth.domain.model.User
import com.replus.api.room.domain.model.Room
import com.replus.api.room.domain.model.RoomMember
import com.replus.api.room.domain.model.RoomRole
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class UserResponse(
    val id: UUID,
    val displayName: String,
    val avatarUrl: String?,
    val isGuest: Boolean,
    val createdAt: Instant,
)

data class UserSummaryResponse(
    val id: UUID,
    val displayName: String,
    val avatarUrl: String?,
)

data class RoomSummaryResponse(
    val id: UUID,
    val name: String,
    val memberCount: Int,
    val maxMembers: Int,
    val currentUserRole: RoomRole,
    val lastMissionDate: LocalDate?,
)

data class RoomMemberResponse(
    val id: UUID,
    val roomId: UUID,
    val user: UserSummaryResponse,
    val role: RoomRole,
    val status: String,
    val joinedAt: Instant,
    val removedAt: Instant?,
)

fun User.toResponse(): UserResponse =
    UserResponse(
        id = id,
        displayName = displayName,
        avatarUrl = avatarUrl,
        isGuest = isGuest,
        createdAt = createdAt,
    )

fun User.toSummaryResponse(): UserSummaryResponse =
    UserSummaryResponse(
        id = id,
        displayName = displayName,
        avatarUrl = avatarUrl,
    )

fun Room.toSummaryResponse(
    memberCount: Int,
    currentMember: RoomMember,
    lastMissionDate: LocalDate?,
): RoomSummaryResponse =
    RoomSummaryResponse(
        id = id,
        name = name,
        memberCount = memberCount,
        maxMembers = maxMembers,
        currentUserRole = currentMember.role,
        lastMissionDate = lastMissionDate,
    )
