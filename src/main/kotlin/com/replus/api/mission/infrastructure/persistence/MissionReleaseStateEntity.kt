package com.replus.api.mission.infrastructure.persistence

import com.replus.api.mission.domain.model.MissionReleaseState
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "mission_release_states")
class MissionReleaseStateEntity(
    @Id
    @Column(name = "mission_id", nullable = false)
    var missionId: UUID,

    @Column(name = "all_submitted_at")
    var allSubmittedAt: Instant?,

    @Column(name = "release_scheduled_at")
    var releaseScheduledAt: Instant?,

    @Column(name = "released_at")
    var releasedAt: Instant?,

    @Column(name = "failed_at")
    var failedAt: Instant?,
) {
    fun toDomain(): MissionReleaseState =
        MissionReleaseState(
            missionId = missionId,
            allSubmittedAt = allSubmittedAt,
            releaseScheduledAt = releaseScheduledAt,
            releasedAt = releasedAt,
            failedAt = failedAt,
        )
}
