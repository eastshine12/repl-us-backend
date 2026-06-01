package com.replus.api.mission.domain.repository

import com.replus.api.mission.domain.model.MissionResponse
import java.util.UUID

interface MissionResponseRepository {
    fun countActiveByMissionId(missionId: UUID): Int

    fun findActiveByMissionId(missionId: UUID): List<MissionResponse>
}
