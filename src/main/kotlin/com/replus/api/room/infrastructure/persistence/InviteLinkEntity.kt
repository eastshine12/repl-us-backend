package com.replus.api.room.infrastructure.persistence

import com.replus.api.room.domain.model.InviteLink
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "invite_links")
class InviteLinkEntity(
    @Id
    @Column(name = "code", nullable = false, length = 32)
    var code: String,

    @Column(name = "room_id", nullable = false)
    var roomId: UUID,

    @Column(name = "created_by_member_id", nullable = false)
    var createdByMemberId: UUID,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(name = "max_uses")
    var maxUses: Int?,

    @Column(name = "uses", nullable = false)
    var uses: Int,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
) {
    fun toDomain(): InviteLink =
        InviteLink(
            code = code,
            roomId = roomId,
            createdByMemberId = createdByMemberId,
            expiresAt = expiresAt,
            maxUses = maxUses,
            uses = uses,
            createdAt = createdAt,
        )

    companion object {
        fun from(inviteLink: InviteLink): InviteLinkEntity =
            InviteLinkEntity(
                code = inviteLink.code,
                roomId = inviteLink.roomId,
                createdByMemberId = inviteLink.createdByMemberId,
                expiresAt = inviteLink.expiresAt,
                maxUses = inviteLink.maxUses,
                uses = inviteLink.uses,
                createdAt = inviteLink.createdAt,
            )
    }
}
