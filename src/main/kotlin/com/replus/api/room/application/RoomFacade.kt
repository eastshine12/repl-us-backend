package com.replus.api.room.application

import com.replus.api.auth.domain.repository.UserRepository
import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType
import com.replus.api.room.domain.model.InviteLink
import com.replus.api.room.domain.model.Room
import com.replus.api.room.domain.model.RoomMember
import com.replus.api.room.domain.model.RoomMemberStatus
import com.replus.api.room.domain.model.RoomRole
import com.replus.api.room.domain.policy.RoomAccessPolicy
import com.replus.api.room.domain.policy.RoomCapacityPolicy
import com.replus.api.room.domain.repository.InviteLinkRepository
import com.replus.api.room.domain.repository.RoomMemberRepository
import com.replus.api.room.domain.repository.RoomRepository
import com.replus.api.room.domain.service.InviteCodeGenerator
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.util.Locale
import java.util.UUID

@Service
class RoomFacade(
    private val userRepository: UserRepository,
    private val roomRepository: RoomRepository,
    private val roomMemberRepository: RoomMemberRepository,
    private val inviteLinkRepository: InviteLinkRepository,
    private val roomAccessPolicy: RoomAccessPolicy,
    private val roomCapacityPolicy: RoomCapacityPolicy,
    private val inviteCodeGenerator: InviteCodeGenerator,
    private val clock: Clock,
    @Value("\${replus.web-base-url:http://localhost:3000}")
    private val webBaseUrl: String,
) {
    @Transactional
    fun createRoom(userId: UUID, name: String): RoomDetailResult {
        val now = clock.instant()
        val roomId = UUID.randomUUID()
        val ownerMemberId = UUID.randomUUID()
        val room = roomRepository.save(
            Room(
                id = roomId,
                name = name.trim(),
                ownerMemberId = ownerMemberId,
                maxMembers = Room.MAX_MEMBERS,
                createdAt = now,
            ),
        )
        roomMemberRepository.save(
            RoomMember(
                id = ownerMemberId,
                roomId = roomId,
                userId = userId,
                role = RoomRole.OWNER,
                status = RoomMemberStatus.ACTIVE,
                slotIndex = 0,
                joinedAt = now,
                removedAt = null,
            ),
        )
        return getRoomDetail(userId, room.id)
    }

    @Transactional(readOnly = true)
    fun getRoomDetail(userId: UUID, roomId: UUID): RoomDetailResult {
        val room = roomRepository.getById(roomId)
        val currentMember = roomMemberRepository.findActiveByRoomIdAndUserId(roomId, userId)
        roomAccessPolicy.requireActiveMember(currentMember)

        val members = roomMemberRepository.findActiveByRoomId(roomId).map { member ->
            RoomMemberResult(
                member = member,
                user = userRepository.getById(member.userId),
            )
        }
        return RoomDetailResult(
            room = room,
            currentMember = currentMember!!,
            members = members,
        )
    }

    @Transactional
    fun createInviteLink(
        userId: UUID,
        roomId: UUID,
        expiresInHours: Long,
        maxUses: Int?,
        rotate: Boolean,
    ): InviteLinkResult {
        val now = clock.instant()
        val member = roomMemberRepository.findActiveByRoomIdAndUserId(roomId, userId)
        roomAccessPolicy.requireActiveMember(member)

        val existing = inviteLinkRepository.findLatestUsableByRoomId(roomId, now)
        if (existing != null && !rotate) {
            return existing.toResult()
        }
        if (existing != null) {
            inviteLinkRepository.save(existing.expire(now))
        }

        val inviteLink = inviteLinkRepository.save(
            InviteLink(
                code = generateUniqueInviteCode(roomId),
                roomId = roomId,
                createdByMemberId = member!!.id,
                expiresAt = now.plus(Duration.ofHours(expiresInHours)),
                maxUses = maxUses,
                uses = 0,
                createdAt = now,
            ),
        )
        return inviteLink.toResult()
    }

    @Transactional
    fun joinByInviteCode(userId: UUID, code: String): RoomDetailResult {
        val now = clock.instant()
        val inviteLink = inviteLinkRepository.findByCode(code.toCanonicalInviteCode())
            ?: throw CoreException(ErrorType.INVITE_LINK_NOT_FOUND)
        if (inviteLink.isExpired(now)) {
            throw CoreException(ErrorType.INVITE_LINK_EXPIRED)
        }
        if (inviteLink.hasReachedUsageLimit()) {
            throw CoreException(ErrorType.INVITE_LINK_USAGE_LIMIT_REACHED)
        }

        val room = roomRepository.getById(inviteLink.roomId)
        val existingMember = roomMemberRepository.findByRoomIdAndUserId(room.id, userId)
        if (existingMember?.isActive() == true) {
            return getRoomDetail(userId, room.id)
        }

        roomCapacityPolicy.ensureCanJoin(
            activeMemberCount = roomMemberRepository.countActiveByRoomId(room.id),
            maxMembers = room.maxMembers,
        )

        val member = if (existingMember != null) {
            existingMember.copy(
                role = RoomRole.MEMBER,
                status = RoomMemberStatus.ACTIVE,
                slotIndex = roomMemberRepository.nextSlotIndex(room.id),
                joinedAt = now,
                removedAt = null,
            )
        } else {
            RoomMember(
                id = UUID.randomUUID(),
                roomId = room.id,
                userId = userId,
                role = RoomRole.MEMBER,
                status = RoomMemberStatus.ACTIVE,
                slotIndex = roomMemberRepository.nextSlotIndex(room.id),
                joinedAt = now,
                removedAt = null,
            )
        }

        roomMemberRepository.save(member)
        inviteLinkRepository.save(inviteLink.recordUse())
        return getRoomDetail(userId, room.id)
    }

    @Transactional
    fun removeMember(userId: UUID, roomId: UUID, memberId: UUID): RemoveMemberResult {
        val now = clock.instant()
        roomRepository.getById(roomId)

        val actor = roomMemberRepository.findActiveByRoomIdAndUserId(roomId, userId)
        roomAccessPolicy.requireActiveMember(actor)
        roomAccessPolicy.requireOwner(actor!!)

        val target = roomMemberRepository.findActiveByIdAndRoomId(memberId, roomId)
            ?: throw CoreException(ErrorType.RESOURCE_NOT_FOUND)
        if (target.isOwner()) {
            throw CoreException(ErrorType.CANNOT_REMOVE_OWNER)
        }

        return RemoveMemberResult(
            member = roomMemberRepository.save(target.remove(now)),
        )
    }

    @Transactional
    fun leaveRoom(userId: UUID, roomId: UUID): RemoveMemberResult {
        val now = clock.instant()
        roomRepository.getById(roomId)

        val member = roomMemberRepository.findActiveByRoomIdAndUserId(roomId, userId)
        roomAccessPolicy.requireActiveMember(member)
        if (member!!.isOwner()) {
            throw CoreException(ErrorType.CANNOT_REMOVE_OWNER)
        }

        return RemoveMemberResult(
            member = roomMemberRepository.save(member.remove(now)),
        )
    }

    private fun generateUniqueInviteCode(roomId: UUID): String {
        repeat(10) {
            val code = inviteCodeGenerator.generate(roomId)
            if (inviteLinkRepository.findByCode(code) == null) {
                return code
            }
        }
        throw CoreException(ErrorType.INTERNAL_ERROR)
    }

    private fun String.toCanonicalInviteCode(): String = trim().uppercase(Locale.ROOT)

    private fun InviteLink.toResult(): InviteLinkResult =
        InviteLinkResult(
            inviteLink = this,
            url = "${webBaseUrl.trimEnd('/')}/join/$code",
        )
}
