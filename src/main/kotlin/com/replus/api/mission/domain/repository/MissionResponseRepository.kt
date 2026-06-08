package com.replus.api.mission.domain.repository

import com.replus.api.mission.domain.model.MissionResponse
import java.util.UUID

interface MissionResponseRepository {
    fun countActiveByRoomId(roomId: UUID): Int

    fun countActiveByMissionId(missionId: UUID): Int

    fun findActiveByMissionId(missionId: UUID): List<MissionResponse>

    fun findActiveByMissionIds(missionIds: Collection<UUID>): List<MissionResponse>

    fun findActiveByMissionIdAndMemberId(missionId: UUID, memberId: UUID): MissionResponse?

    fun findByMissionIdAndMemberId(missionId: UUID, memberId: UUID): MissionResponse?

    fun findActiveByIdAndRoomId(responseId: UUID, roomId: UUID): MissionResponse?

    fun save(response: MissionResponse): MissionResponse
}
