package com.replus.api.mission.domain.repository

import com.replus.api.mission.domain.model.MissionResponse
import java.util.UUID

interface MissionResponseRepository {
    fun countActiveByMissionId(missionId: UUID): Int

    fun findActiveByMissionId(missionId: UUID): List<MissionResponse>

    fun findActiveByMissionIdAndMemberId(missionId: UUID, memberId: UUID): MissionResponse?

    fun save(response: MissionResponse): MissionResponse
}
