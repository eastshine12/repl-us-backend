package com.replus.api.mission.domain.model

import java.time.Instant
import java.util.UUID

data class MissionReleaseState(
    val missionId: UUID,
    val allSubmittedAt: Instant?,
    val releaseScheduledAt: Instant?,
    val releasedAt: Instant?,
    val failedAt: Instant?,
)
