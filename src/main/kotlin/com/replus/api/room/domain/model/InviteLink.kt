package com.replus.api.room.domain.model

import java.time.Instant
import java.util.UUID

data class InviteLink(
    val code: String,
    val roomId: UUID,
    val createdByMemberId: UUID,
    val expiresAt: Instant,
    val maxUses: Int?,
    val uses: Int,
    val createdAt: Instant,
) {
    fun isUsable(now: Instant): Boolean =
        !isExpired(now) && !hasReachedUsageLimit()

    fun isExpired(now: Instant): Boolean = !now.isBefore(expiresAt)

    fun hasReachedUsageLimit(): Boolean = maxUses != null && uses >= maxUses

    fun recordUse(): InviteLink = copy(uses = uses + 1)
}
