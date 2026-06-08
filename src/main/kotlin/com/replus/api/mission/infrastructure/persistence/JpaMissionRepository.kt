package com.replus.api.mission.infrastructure.persistence

import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType
import com.replus.api.mission.domain.model.Mission
import com.replus.api.mission.domain.repository.MissionRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Repository
class JpaMissionRepository(
    private val missionJpaRepository: MissionJpaRepository,
    private val entityManager: EntityManager,
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

    @Transactional
    override fun save(mission: Mission): Mission {
        val saved = missionJpaRepository.saveAndFlush(MissionEntity.from(mission))
        entityManager.refresh(saved)
        return saved.toDomain()
    }
}
