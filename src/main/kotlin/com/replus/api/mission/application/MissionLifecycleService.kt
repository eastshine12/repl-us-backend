package com.replus.api.mission.application

import com.replus.api.mission.domain.model.Mission
import com.replus.api.mission.domain.model.MissionReleaseState
import com.replus.api.mission.domain.repository.MissionReleaseStateRepository
import com.replus.api.mission.domain.repository.MissionRepository
import com.replus.api.mission.domain.repository.MissionResponseRepository
import com.replus.api.room.domain.repository.RoomMemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

@Service
class MissionLifecycleService(
    private val missionRepository: MissionRepository,
    private val missionResponseRepository: MissionResponseRepository,
    private val missionReleaseStateRepository: MissionReleaseStateRepository,
    private val roomMemberRepository: RoomMemberRepository,
    private val clock: Clock,
) : MissionLifecycleFailureUseCase {
    @Transactional
    override fun failIncompleteMissionsBefore(cutoffDate: LocalDate): MissionLifecycleFailureResult {
        val now = clock.instant()
        val failedMissionIds = missionRepository
            .findAllByMissionDateBefore(cutoffDate)
            .filter { shouldFail(it) }
            .map { mission ->
                val releaseState = missionReleaseStateRepository.findByMissionId(mission.id)
                missionReleaseStateRepository.save(
                    releaseState?.copy(failedAt = now) ?: MissionReleaseState(
                        missionId = mission.id,
                        allSubmittedAt = null,
                        releaseScheduledAt = null,
                        releasedAt = null,
                        failedAt = now,
                    ),
                )
                mission.id
            }

        return MissionLifecycleFailureResult(failedMissionIds = failedMissionIds)
    }

    private fun shouldFail(mission: Mission): Boolean {
        val releaseState = missionReleaseStateRepository.findByMissionId(mission.id)
        if (releaseState?.failedAt != null ||
            releaseState?.releasedAt != null ||
            releaseState?.allSubmittedAt != null ||
            releaseState?.releaseScheduledAt != null
        ) {
            return false
        }

        val activeMemberCount = roomMemberRepository.countActiveByRoomId(mission.roomId)
        val submittedCount = missionResponseRepository.countActiveByMissionId(mission.id)
        return activeMemberCount > 0 && submittedCount < activeMemberCount
    }
}

data class MissionLifecycleFailureResult(
    val failedMissionIds: List<UUID>,
)

fun interface MissionLifecycleFailureUseCase {
    fun failIncompleteMissionsBefore(cutoffDate: LocalDate): MissionLifecycleFailureResult
}
