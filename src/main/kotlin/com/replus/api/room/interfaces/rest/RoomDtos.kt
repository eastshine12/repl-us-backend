package com.replus.api.room.interfaces.rest

import com.replus.api.common.interfaces.rest.dto.RoomMemberResponse
import com.replus.api.common.interfaces.rest.dto.UserSummaryResponse
import com.replus.api.common.interfaces.rest.dto.toSummaryResponse
import com.replus.api.room.application.InviteLinkResult
import com.replus.api.room.application.RoomDetailResult
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateRoomRequest(
    @field:NotBlank
    @field:Size(max = 32)
    val name: String,
)

data class CreateInviteLinkRequest(
    @field:Min(1)
    @field:Max(720)
    val expiresInHours: Long? = 168,

    @field:Min(1)
    @field:Max(5)
    val maxUses: Int? = null,
)

data class RoomDetailResponse(
    val id: UUID,
    val name: String,
    val memberCount: Int,
    val maxMembers: Int,
    val ownerMemberId: UUID,
    val currentUserMemberId: UUID,
    val currentUserRole: String,
    val members: List<RoomMemberResponse>,
    val createdAt: Instant,
)

data class InviteLinkResponse(
    val code: String,
    val roomId: UUID,
    val url: String,
    val expiresAt: Instant,
    val maxUses: Int?,
    val uses: Int,
)

fun RoomDetailResult.toResponse(): RoomDetailResponse =
    RoomDetailResponse(
        id = room.id,
        name = room.name,
        memberCount = members.size,
        maxMembers = room.maxMembers,
        ownerMemberId = room.ownerMemberId,
        currentUserMemberId = currentMember.id,
        currentUserRole = currentMember.role.name,
        members = members.map {
            RoomMemberResponse(
                id = it.member.id,
                roomId = it.member.roomId,
                user = UserSummaryResponse(
                    id = it.user.id,
                    displayName = it.user.displayName,
                    avatarUrl = it.user.avatarUrl,
                ),
                role = it.member.role,
                status = it.member.status.name,
                joinedAt = it.member.joinedAt,
                removedAt = it.member.removedAt,
            )
        },
        createdAt = room.createdAt,
    )

fun InviteLinkResult.toResponse(): InviteLinkResponse =
    InviteLinkResponse(
        code = inviteLink.code,
        roomId = inviteLink.roomId,
        url = url,
        expiresAt = inviteLink.expiresAt,
        maxUses = inviteLink.maxUses,
        uses = inviteLink.uses,
    )
