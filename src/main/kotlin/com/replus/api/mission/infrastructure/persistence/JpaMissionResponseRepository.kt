package com.replus.api.mission.infrastructure.persistence

import com.replus.api.mission.domain.model.MissionResponse
import com.replus.api.mission.domain.model.MissionResponseStatus
import com.replus.api.mission.domain.repository.MissionResponseRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaMissionResponseRepository(
    private val missionResponseJpaRepository: MissionResponseJpaRepository,
) : MissionResponseRepository {
    override fun countActiveByMissionId(missionId: UUID): Int =
        missionResponseJpaRepository.countByMissionIdAndStatus(missionId, MissionResponseStatus.ACTIVE)

    override fun findActiveByMissionId(missionId: UUID): List<MissionResponse> =
        missionResponseJpaRepository
            .findAllByMissionIdAndStatus(missionId, MissionResponseStatus.ACTIVE)
            .map { it.toDomain() }
}
