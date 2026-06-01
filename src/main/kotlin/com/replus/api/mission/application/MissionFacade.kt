package com.replus.api.mission.application

import com.replus.api.auth.domain.repository.UserRepository
import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType
import com.replus.api.mission.domain.model.Mission
import com.replus.api.mission.domain.model.MissionCategory
import com.replus.api.mission.domain.policy.MissionEditPolicy
import com.replus.api.mission.domain.repository.MissionReleaseStateRepository
import com.replus.api.mission.domain.repository.MissionRepository
import com.replus.api.mission.domain.repository.MissionResponseRepository
import com.replus.api.room.domain.policy.RoomAccessPolicy
import com.replus.api.room.domain.repository.RoomMemberRepository
import com.replus.api.room.domain.repository.RoomRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Service
class MissionFacade(
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository,
    private val roomMemberRepository: RoomMemberRepository,
    private val missionRepository: MissionRepository,
    private val missionResponseRepository: MissionResponseRepository,
    private val missionReleaseStateRepository: MissionReleaseStateRepository,
    private val roomAccessPolicy: RoomAccessPolicy,
    private val missionEditPolicy: MissionEditPolicy,
    private val clock: Clock,
) {
    @Transactional
    fun getToday(userId: UUID, roomId: UUID): TodayResult {
        val room = roomRepository.getById(roomId)
        val currentMember = roomMemberRepository.findActiveByRoomIdAndUserId(roomId, userId)
        roomAccessPolicy.requireActiveMember(currentMember)

        val today = today()
        val mission = missionRepository.findByRoomIdAndMissionDate(roomId, today)
            ?: missionRepository.save(defaultMission(roomId, today))
        val activeMembers = roomMemberRepository.findActiveByRoomId(roomId)
        val memberResults = activeMembers.map { TodayMemberResult(it, userRepository.getById(it.userId)) }
        val responses = missionResponseRepository.findActiveByMissionId(mission.id)
        val viewerResponse = responses.firstOrNull { it.memberId == currentMember!!.id }
        val releaseState = missionReleaseStateRepository.findByMissionId(mission.id)
        val allSubmitted = responses.size == activeMembers.size && activeMembers.isNotEmpty()
        val canViewFriendResponses = releaseState?.releasedAt?.let { !clock.instant().isBefore(it) } ?: false

        return TodayResult(
            serverDate = today,
            room = room,
            viewer = ViewerStateResult(
                memberId = currentMember!!.id,
                role = currentMember.role.name,
                hasSubmittedToday = viewerResponse != null,
                todayResponseId = viewerResponse?.id,
            ),
            currentMember = currentMember,
            members = memberResults,
            memberCount = activeMembers.size,
            mission = mission,
            participation = ParticipationResult(
                totalActiveMembers = activeMembers.size,
                submittedCount = responses.size,
                viewerHasSubmitted = viewerResponse != null,
                canViewFriendResponses = canViewFriendResponses,
                allSubmitted = allSubmitted,
            ),
            responses = responses,
            releaseState = releaseState,
        )
    }

    @Transactional
    fun updateTodayMission(
        userId: UUID,
        roomId: UUID,
        missionId: UUID,
        prompt: String,
        category: MissionCategory,
    ): Mission {
        val member = roomMemberRepository.findActiveByRoomIdAndUserId(roomId, userId)
        roomAccessPolicy.requireActiveMember(member)
        val mission = missionRepository.getByIdAndRoomId(missionId, roomId)
        if (mission.missionDate != today()) {
            throw CoreException(ErrorType.RESOURCE_NOT_FOUND)
        }

        val activeResponseCount = missionResponseRepository.countActiveByMissionId(mission.id)
        missionEditPolicy.validateTodayEdit(member!!, mission, activeResponseCount)

        return missionRepository.save(
            mission.edit(
                prompt = prompt.trim(),
                category = category,
                editorMemberId = member.id,
                editedAt = clock.instant(),
            ),
        )
    }

    private fun defaultMission(roomId: UUID, missionDate: LocalDate): Mission =
        Mission(
            id = UUID.randomUUID(),
            roomId = roomId,
            missionDate = missionDate,
            prompt = "요즘 나만 알고 싶은 음료 조합은?",
            category = MissionCategory.OBSERVATION,
            editCount = 0,
            editedByMemberId = null,
            editedAt = null,
            createdAt = clock.instant(),
        )

    private fun today(): LocalDate =
        LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul")))
}
