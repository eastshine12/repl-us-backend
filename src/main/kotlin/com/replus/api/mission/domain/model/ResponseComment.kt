package com.replus.api.mission.domain.model

import java.time.Instant
import java.util.UUID

data class ResponseComment(
    val id: UUID,
    val responseId: UUID,
    val memberId: UUID,
    val body: String,
    val createdAt: Instant,
    val deletedAt: Instant?,
)
