package com.replus.api.room.infrastructure.persistence

import com.replus.api.room.domain.model.RoomMember
import com.replus.api.room.domain.model.RoomMemberStatus
import com.replus.api.room.domain.repository.RoomMemberRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaRoomMemberRepository(
    private val roomMemberJpaRepository: RoomMemberJpaRepository,
) : RoomMemberRepository {
    override fun findActiveByRoomIdAndUserId(roomId: UUID, userId: UUID): RoomMember? =
        roomMemberJpaRepository.findByRoomIdAndUserIdAndStatus(roomId, userId, RoomMemberStatus.ACTIVE)
            ?.toDomain()

    override fun findActiveByIdAndRoomId(memberId: UUID, roomId: UUID): RoomMember? =
        roomMemberJpaRepository.findByIdAndRoomIdAndStatus(memberId, roomId, RoomMemberStatus.ACTIVE)
            ?.toDomain()

    override fun findByRoomIdAndUserId(roomId: UUID, userId: UUID): RoomMember? =
        roomMemberJpaRepository.findByRoomIdAndUserId(roomId, userId)?.toDomain()

    override fun findActiveByRoomId(roomId: UUID): List<RoomMember> =
        roomMemberJpaRepository
            .findAllByRoomIdAndStatusOrderBySlotIndexAsc(roomId, RoomMemberStatus.ACTIVE)
            .map { it.toDomain() }

    override fun findActiveByUserId(userId: UUID): List<RoomMember> =
        roomMemberJpaRepository
            .findAllByUserIdAndStatusOrderByJoinedAtDesc(userId, RoomMemberStatus.ACTIVE)
            .map { it.toDomain() }

    override fun countActiveByRoomId(roomId: UUID): Int =
        roomMemberJpaRepository.countByRoomIdAndStatus(roomId, RoomMemberStatus.ACTIVE)

    override fun nextSlotIndex(roomId: UUID): Int =
        (roomMemberJpaRepository.findFirstByRoomIdOrderBySlotIndexDesc(roomId)?.slotIndex ?: -1) + 1

    override fun save(member: RoomMember): RoomMember =
        roomMemberJpaRepository.save(RoomMemberEntity.from(member)).toDomain()
}
