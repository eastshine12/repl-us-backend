package com.replus.api.mission.domain.repository

import com.replus.api.mission.domain.model.Mission
import java.time.LocalDate
import java.util.UUID

interface MissionRepository {
    fun findByRoomIdAndMissionDate(roomId: UUID, missionDate: LocalDate): Mission?

    fun findAllByRoomId(roomId: UUID): List<Mission>

    fun findAllByMissionDateBefore(cutoffDate: LocalDate): List<Mission>

    fun findAllByRoomIdAndMissionDateBetween(
        roomId: UUID,
        from: LocalDate,
        to: LocalDate,
    ): List<Mission>

    fun findLatestByRoomId(roomId: UUID): Mission?

    fun getByIdAndRoomId(missionId: UUID, roomId: UUID): Mission

    fun save(mission: Mission): Mission
}
