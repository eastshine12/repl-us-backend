package com.replus.api.common.infrastructure

import com.replus.api.auth.domain.model.User
import com.replus.api.auth.domain.repository.UserRepository
import com.replus.api.common.security.DevSessionStore
import com.replus.api.room.domain.model.InviteLink
import com.replus.api.room.domain.model.Room
import com.replus.api.room.domain.model.RoomMember
import com.replus.api.room.domain.model.RoomMemberStatus
import com.replus.api.room.domain.model.RoomRole
import com.replus.api.room.domain.repository.InviteLinkRepository
import com.replus.api.room.domain.repository.RoomMemberRepository
import com.replus.api.room.domain.repository.RoomRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Component
class LocalDevDataSeeder(
    private val userRepository: UserRepository,
    private val roomRepository: RoomRepository,
    private val roomMemberRepository: RoomMemberRepository,
    private val inviteLinkRepository: InviteLinkRepository,
    private val clock: Clock,
    @Value("\${replus.seed-dev-data:true}")
    private val seedDevData: Boolean,
) : ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments) {
        if (!seedDevData || userRepository.existsById(DevSessionStore.MINA_USER_ID)) {
            return
        }

        val now = clock.instant()
        val roomId = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa")
        val minaMemberId = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb1")
        val joonMemberId = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb2")
        val araMemberId = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb3")

        userRepository.save(user(DevSessionStore.MINA_USER_ID, "민아", now))
        userRepository.save(user(DevSessionStore.JOON_USER_ID, "준", now))
        userRepository.save(user(DevSessionStore.ARA_USER_ID, "아라", now))
        roomRepository.save(
            Room(
                id = roomId,
                name = "책상 위 소동방",
                ownerMemberId = minaMemberId,
                createdAt = now,
            ),
        )
        roomMemberRepository.save(member(minaMemberId, roomId, DevSessionStore.MINA_USER_ID, RoomRole.OWNER, 0, now))
        roomMemberRepository.save(member(joonMemberId, roomId, DevSessionStore.JOON_USER_ID, RoomRole.MEMBER, 1, now))
        roomMemberRepository.save(member(araMemberId, roomId, DevSessionStore.ARA_USER_ID, RoomRole.MEMBER, 2, now))
        inviteLinkRepository.save(
            InviteLink(
                code = "R3S9KQ",
                roomId = roomId,
                createdByMemberId = minaMemberId,
                expiresAt = now.plus(Duration.ofDays(7)),
                maxUses = 5,
                uses = 0,
                createdAt = now,
            ),
        )
    }

    private fun user(id: UUID, displayName: String, now: Instant): User =
        User(
            id = id,
            displayName = displayName,
            avatarUrl = null,
            isGuest = false,
            createdAt = now,
        )

    private fun member(
        id: UUID,
        roomId: UUID,
        userId: UUID,
        role: RoomRole,
        slotIndex: Int,
        now: Instant,
    ): RoomMember =
        RoomMember(
            id = id,
            roomId = roomId,
            userId = userId,
            role = role,
            status = RoomMemberStatus.ACTIVE,
            slotIndex = slotIndex,
            joinedAt = now,
            removedAt = null,
        )
}
