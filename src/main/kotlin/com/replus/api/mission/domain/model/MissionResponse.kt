package com.replus.api.mission.domain.model

import java.time.Instant
import java.util.UUID

data class MissionResponse(
    val id: UUID,
    val roomId: UUID,
    val missionId: UUID,
    val memberId: UUID,
    val videoAssetId: UUID,
    val status: MissionResponseStatus,
    val createdAt: Instant,
    val deletedAt: Instant?,
) {
    fun isActive(): Boolean = status == MissionResponseStatus.ACTIVE

    fun delete(deletedAt: Instant): MissionResponse =
        copy(
            status = MissionResponseStatus.DELETED,
            deletedAt = deletedAt,
        )
}

enum class MissionResponseStatus {
    ACTIVE,
    DELETED,
}
