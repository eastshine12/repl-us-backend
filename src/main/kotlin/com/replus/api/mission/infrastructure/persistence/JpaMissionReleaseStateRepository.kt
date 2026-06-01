package com.replus.api.mission.infrastructure.persistence

import com.replus.api.mission.domain.model.MissionReleaseState
import com.replus.api.mission.domain.repository.MissionReleaseStateRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaMissionReleaseStateRepository(
    private val missionReleaseStateJpaRepository: MissionReleaseStateJpaRepository,
) : MissionReleaseStateRepository {
    override fun findByMissionId(missionId: UUID): MissionReleaseState? =
        missionReleaseStateJpaRepository.findByIdOrNull(missionId)?.toDomain()
}
