package com.replus.api.mission.infrastructure.persistence

import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType
import com.replus.api.mission.domain.model.Mission
import com.replus.api.mission.domain.repository.MissionRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
class JpaMissionRepository(
    private val missionJpaRepository: MissionJpaRepository,
) : MissionRepository {
    override fun findByRoomIdAndMissionDate(roomId: UUID, missionDate: LocalDate): Mission? =
        missionJpaRepository.findByRoomIdAndMissionDate(roomId, missionDate)?.toDomain()

    override fun findAllByRoomId(roomId: UUID): List<Mission> =
        missionJpaRepository.findAllByRoomIdOrderByMissionDateDesc(roomId).map { it.toDomain() }

    override fun findAllByMissionDateBefore(cutoffDate: LocalDate): List<Mission> =
        missionJpaRepository.findAllByMissionDateBeforeOrderByMissionDateAsc(cutoffDate).map { it.toDomain() }

    override fun findAllByRoomIdAndMissionDateBetween(
        roomId: UUID,
        from: LocalDate,
        to: LocalDate,
    ): List<Mission> =
        missionJpaRepository.findAllByRoomIdAndMissionDateBetweenOrderByMissionDateDesc(roomId, from, to)
            .map { it.toDomain() }

    override fun findLatestByRoomId(roomId: UUID): Mission? =
        missionJpaRepository.findFirstByRoomIdOrderByMissionDateDesc(roomId)?.toDomain()

    override fun getByIdAndRoomId(missionId: UUID, roomId: UUID): Mission =
        missionJpaRepository.findByIdAndRoomId(missionId, roomId)?.toDomain()
            ?: throw CoreException(ErrorType.RESOURCE_NOT_FOUND)

    override fun save(mission: Mission): Mission =
        missionJpaRepository.save(MissionEntity.from(mission)).toDomain()
}
