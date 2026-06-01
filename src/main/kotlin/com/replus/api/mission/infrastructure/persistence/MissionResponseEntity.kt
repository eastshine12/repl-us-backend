package com.replus.api.mission.infrastructure.persistence

import com.replus.api.mission.domain.model.MissionResponse
import com.replus.api.mission.domain.model.MissionResponseStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "mission_responses")
class MissionResponseEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "room_id", nullable = false)
    var roomId: UUID,

    @Column(name = "mission_id", nullable = false)
    var missionId: UUID,

    @Column(name = "member_id", nullable = false)
    var memberId: UUID,

    @Column(name = "video_asset_id", nullable = false)
    var videoAssetId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    var status: MissionResponseStatus,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "deleted_at")
    var deletedAt: Instant?,
) {
    fun toDomain(): MissionResponse =
        MissionResponse(
            id = id,
            roomId = roomId,
            missionId = missionId,
            memberId = memberId,
            videoAssetId = videoAssetId,
            status = status,
            createdAt = createdAt,
            deletedAt = deletedAt,
        )
}
