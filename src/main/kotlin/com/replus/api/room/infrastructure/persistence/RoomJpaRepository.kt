package com.replus.api.room.infrastructure.persistence

import com.replus.api.room.domain.model.RoomMemberStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface RoomJpaRepository : JpaRepository<RoomEntity, UUID>

interface RoomMemberJpaRepository : JpaRepository<RoomMemberEntity, UUID> {
    fun findByRoomIdAndUserIdAndStatus(
        roomId: UUID,
        userId: UUID,
        status: RoomMemberStatus,
    ): RoomMemberEntity?

    fun findByRoomIdAndUserId(roomId: UUID, userId: UUID): RoomMemberEntity?

    fun findAllByRoomIdAndStatusOrderBySlotIndexAsc(
        roomId: UUID,
        status: RoomMemberStatus,
    ): List<RoomMemberEntity>

    fun findAllByUserIdAndStatusOrderByJoinedAtDesc(
        userId: UUID,
        status: RoomMemberStatus,
    ): List<RoomMemberEntity>

    fun countByRoomIdAndStatus(roomId: UUID, status: RoomMemberStatus): Int

    fun findFirstByRoomIdOrderBySlotIndexDesc(roomId: UUID): RoomMemberEntity?
}

interface InviteLinkJpaRepository : JpaRepository<InviteLinkEntity, String> {
    fun findFirstByRoomIdAndExpiresAtAfterOrderByCreatedAtDesc(
        roomId: UUID,
        now: Instant,
    ): InviteLinkEntity?
}
