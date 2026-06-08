package com.replus.api.mission.domain.repository

import com.replus.api.mission.domain.model.MissionReleaseState
import java.time.Instant
import java.util.UUID

interface MissionReleaseStateRepository {
    fun findByMissionId(missionId: UUID): MissionReleaseState?

    fun findAllByMissionIds(missionIds: Collection<UUID>): List<MissionReleaseState>

    fun findAllDueForRelease(dueAt: Instant): List<MissionReleaseState>

    fun save(releaseState: MissionReleaseState): MissionReleaseState
}
