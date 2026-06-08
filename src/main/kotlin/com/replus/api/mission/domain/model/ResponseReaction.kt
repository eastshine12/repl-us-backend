package com.replus.api.mission.domain.model

import java.time.Instant
import java.util.UUID

data class ResponseReaction(
    val id: UUID,
    val responseId: UUID,
    val memberId: UUID,
    val type: ReactionType,
    val createdAt: Instant,
)

enum class ReactionType {
    LAUGH,
    HEART,
    WOW,
}
