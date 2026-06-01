package com.replus.api.mission.infrastructure.persistence

import com.replus.api.mission.domain.model.MissionResponseStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface MissionJpaRepository : JpaRepository<MissionEntity, UUID> {
    fun findByRoomIdAndMissionDate(roomId: UUID, missionDate: LocalDate): MissionEntity?

    fun findFirstByRoomIdOrderByMissionDateDesc(roomId: UUID): MissionEntity?

    fun findByIdAndRoomId(id: UUID, roomId: UUID): MissionEntity?
}

interface MissionResponseJpaRepository : JpaRepository<MissionResponseEntity, UUID> {
    fun countByMissionIdAndStatus(missionId: UUID, status: MissionResponseStatus): Int

    fun findAllByMissionIdAndStatus(
        missionId: UUID,
        status: MissionResponseStatus,
    ): List<MissionResponseEntity>

    fun findByMissionIdAndMemberIdAndStatus(
        missionId: UUID,
        memberId: UUID,
        status: MissionResponseStatus,
    ): MissionResponseEntity?
}

interface VideoAssetJpaRepository : JpaRepository<VideoAssetEntity, UUID>

interface MissionReleaseStateJpaRepository : JpaRepository<MissionReleaseStateEntity, UUID>
