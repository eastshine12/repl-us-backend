package com.replus.api.room.infrastructure.persistence

import com.replus.api.room.domain.model.RoomMember
import com.replus.api.room.domain.model.RoomMemberStatus
import com.replus.api.room.domain.model.RoomRole
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "room_members")
class RoomMemberEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "room_id", nullable = false)
    var roomId: UUID,

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 16)
    var role: RoomRole,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    var status: RoomMemberStatus,

    @Column(name = "slot_index", nullable = false)
    var slotIndex: Int,

    @Column(name = "joined_at", nullable = false)
    var joinedAt: Instant,

    @Column(name = "removed_at")
    var removedAt: Instant?,
) {
    fun toDomain(): RoomMember =
        RoomMember(
            id = id,
            roomId = roomId,
            userId = userId,
            role = role,
            status = status,
            slotIndex = slotIndex,
            joinedAt = joinedAt,
            removedAt = removedAt,
        )

    companion object {
        fun from(member: RoomMember): RoomMemberEntity =
            RoomMemberEntity(
                id = member.id,
                roomId = member.roomId,
                userId = member.userId,
                role = member.role,
                status = member.status,
                slotIndex = member.slotIndex,
                joinedAt = member.joinedAt,
                removedAt = member.removedAt,
            )
    }
}
