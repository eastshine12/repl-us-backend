package com.replus.api.room.domain.repository

import com.replus.api.room.domain.model.InviteLink
import java.time.Instant
import java.util.UUID

interface InviteLinkRepository {
    fun findByCode(code: String): InviteLink?

    fun findLatestUsableByRoomId(roomId: UUID, now: Instant): InviteLink?

    fun save(inviteLink: InviteLink): InviteLink
}
