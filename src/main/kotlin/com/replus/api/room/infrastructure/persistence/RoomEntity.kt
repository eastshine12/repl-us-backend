package com.replus.api.room.infrastructure.persistence

import com.replus.api.room.domain.model.Room
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "rooms")
class RoomEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "name", nullable = false, length = 32)
    var name: String,

    @Column(name = "owner_member_id", nullable = false)
    var ownerMemberId: UUID,

    @Column(name = "max_members", nullable = false)
    var maxMembers: Int,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
) {
    fun toDomain(): Room =
        Room(
            id = id,
            name = name,
            ownerMemberId = ownerMemberId,
            maxMembers = maxMembers,
            createdAt = createdAt,
        )

    companion object {
        fun from(room: Room): RoomEntity =
            RoomEntity(
                id = room.id,
                name = room.name,
                ownerMemberId = room.ownerMemberId,
                maxMembers = room.maxMembers,
                createdAt = room.createdAt,
            )
    }
}
