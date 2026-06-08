package com.replus.api.mission.infrastructure.persistence

import com.replus.api.mission.domain.model.MissionReleaseState
import com.replus.api.mission.domain.repository.MissionReleaseStateRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
class JpaMissionReleaseStateRepository(
    private val missionReleaseStateJpaRepository: MissionReleaseStateJpaRepository,
) : MissionReleaseStateRepository {
    override fun findByMissionId(missionId: UUID): MissionReleaseState? =
        missionReleaseStateJpaRepository.findByIdOrNull(missionId)?.toDomain()

    override fun findAllByMissionIds(missionIds: Collection<UUID>): List<MissionReleaseState> =
        if (missionIds.isEmpty()) {
            emptyList()
        } else {
            missionReleaseStateJpaRepository.findAllById(missionIds).map { it.toDomain() }
        }

    override fun findAllDueForRelease(dueAt: Instant): List<MissionReleaseState> =
        missionReleaseStateJpaRepository
            .findAllByReleaseScheduledAtLessThanEqualAndReleasedAtIsNullAndFailedAtIsNull(dueAt)
            .map { it.toDomain() }

    override fun save(releaseState: MissionReleaseState): MissionReleaseState =
        missionReleaseStateJpaRepository.save(MissionReleaseStateEntity.from(releaseState)).toDomain()
}
