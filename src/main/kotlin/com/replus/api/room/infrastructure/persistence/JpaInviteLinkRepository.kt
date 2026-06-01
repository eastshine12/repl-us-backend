package com.replus.api.room.infrastructure.persistence

import com.replus.api.room.domain.model.InviteLink
import com.replus.api.room.domain.repository.InviteLinkRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
class JpaInviteLinkRepository(
    private val inviteLinkJpaRepository: InviteLinkJpaRepository,
) : InviteLinkRepository {
    override fun findByCode(code: String): InviteLink? =
        inviteLinkJpaRepository.findByIdOrNull(code)?.toDomain()

    override fun findLatestUsableByRoomId(roomId: UUID, now: Instant): InviteLink? =
        inviteLinkJpaRepository
            .findFirstByRoomIdAndExpiresAtAfterOrderByCreatedAtDesc(roomId, now)
            ?.toDomain()
            ?.takeIf { it.isUsable(now) }

    override fun save(inviteLink: InviteLink): InviteLink =
        inviteLinkJpaRepository.save(InviteLinkEntity.from(inviteLink)).toDomain()
}
