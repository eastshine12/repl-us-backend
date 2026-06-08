package com.replus.api.room.domain.repository

import com.replus.api.room.domain.model.RoomMember
import java.util.UUID

interface RoomMemberRepository {
    fun findActiveByRoomIdAndUserId(roomId: UUID, userId: UUID): RoomMember?

    fun findActiveByIdAndRoomId(memberId: UUID, roomId: UUID): RoomMember?

    fun findByRoomIdAndUserId(roomId: UUID, userId: UUID): RoomMember?

    fun findActiveByRoomId(roomId: UUID): List<RoomMember>

    fun findActiveByUserId(userId: UUID): List<RoomMember>

    fun countActiveByRoomId(roomId: UUID): Int

    fun nextSlotIndex(roomId: UUID): Int

    fun save(member: RoomMember): RoomMember
}
