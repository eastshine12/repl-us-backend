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

    override fun findActiveByMissionIdAndMemberId(missionId: UUID, memberId: UUID): MissionResponse? =
        missionResponseJpaRepository
            .findByMissionIdAndMemberIdAndStatus(missionId, memberId, MissionResponseStatus.ACTIVE)
            ?.toDomain()

    override fun findByMissionIdAndMemberId(missionId: UUID, memberId: UUID): MissionResponse? =
        missionResponseJpaRepository.findByMissionIdAndMemberId(missionId, memberId)?.toDomain()

    override fun findActiveByIdAndRoomId(responseId: UUID, roomId: UUID): MissionResponse? =
        missionResponseJpaRepository
            .findByIdAndRoomIdAndStatus(responseId, roomId, MissionResponseStatus.ACTIVE)
            ?.toDomain()

    override fun save(response: MissionResponse): MissionResponse =
        missionResponseJpaRepository.save(MissionResponseEntity.from(response)).toDomain()
}
