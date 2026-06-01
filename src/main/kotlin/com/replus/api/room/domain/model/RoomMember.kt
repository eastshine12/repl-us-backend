package com.replus.api.room.domain.model

import java.time.Instant
import java.util.UUID

data class RoomMember(
    val id: UUID,
    val roomId: UUID,
    val userId: UUID,
    val role: RoomRole,
    val status: RoomMemberStatus,
    val slotIndex: Int,
    val joinedAt: Instant,
    val removedAt: Instant?,
) {
    fun isActive(): Boolean = status == RoomMemberStatus.ACTIVE

    fun isOwner(): Boolean = role == RoomRole.OWNER
}

enum class RoomRole {
    OWNER,
    MEMBER,
}

enum class RoomMemberStatus {
    ACTIVE,
    REMOVED,
}
