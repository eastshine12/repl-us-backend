package com.replus.api.mission.application

import com.replus.api.auth.domain.repository.UserRepository
import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType
import com.replus.api.mission.domain.model.MissionReleaseState
import com.replus.api.mission.domain.model.MissionResponse
import com.replus.api.mission.domain.model.ReactionType
import com.replus.api.mission.domain.model.ResponseComment
import com.replus.api.mission.domain.model.ResponseReaction
import com.replus.api.mission.domain.repository.MissionReleaseStateRepository
import com.replus.api.mission.domain.repository.MissionResponseRepository
import com.replus.api.mission.domain.repository.ResponseCommentRepository
import com.replus.api.mission.domain.repository.ResponseReactionRepository
import com.replus.api.room.domain.model.RoomMember
import com.replus.api.room.domain.policy.RoomAccessPolicy
import com.replus.api.room.domain.repository.RoomMemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.UUID

@Service
class ResponseInteractionFacade(
    private val userRepository: UserRepository,
    private val roomMemberRepository: RoomMemberRepository,
    private val missionResponseRepository: MissionResponseRepository,
    private val missionReleaseStateRepository: MissionReleaseStateRepository,
    private val responseReactionRepository: ResponseReactionRepository,
    private val responseCommentRepository: ResponseCommentRepository,
    private val roomAccessPolicy: RoomAccessPolicy,
    private val clock: Clock,
) {
    @Transactional
    fun createReaction(
        userId: UUID,
        roomId: UUID,
        responseId: UUID,
        type: ReactionType,
    ): CreatedReactionResult {
        val member = requireActiveMember(roomId, userId)
        val response = requireVisibleResponse(roomId, responseId, member)
        val reaction = responseReactionRepository.save(
            ResponseReaction(
                id = UUID.randomUUID(),
                responseId = response.id,
                memberId = member.id,
                type = type,
                createdAt = clock.instant(),
            ),
        )

        return CreatedReactionResult(reaction)
    }

    @Transactional
    fun createComment(
        userId: UUID,
        roomId: UUID,
        responseId: UUID,
        body: String,
    ): CreatedCommentResult {
        val member = requireActiveMember(roomId, userId)
        val response = requireVisibleResponse(roomId, responseId, member)
        val comment = responseCommentRepository.save(
            ResponseComment(
                id = UUID.randomUUID(),
                responseId = response.id,
                memberId = member.id,
                body = body.trim(),
                createdAt = clock.instant(),
                deletedAt = null,
            ),
        )

        return CreatedCommentResult(
            comment = comment,
            author = userRepository.getById(member.userId),
        )
    }

    private fun requireActiveMember(roomId: UUID, userId: UUID): RoomMember =
        roomMemberRepository.findActiveByRoomIdAndUserId(roomId, userId)
            .also { roomAccessPolicy.requireActiveMember(it) }!!

    private fun requireVisibleResponse(roomId: UUID, responseId: UUID, member: RoomMember): MissionResponse {
        val response = missionResponseRepository.findActiveByIdAndRoomId(responseId, roomId)
            ?: throw CoreException(ErrorType.RESOURCE_NOT_FOUND)
        if (response.memberId == member.id) {
            return response
        }

        val releaseState = releaseIfDue(missionReleaseStateRepository.findByMissionId(response.missionId))
        val releasedAt = releaseState?.releasedAt
        if (releasedAt == null || clock.instant().isBefore(releasedAt)) {
            throw CoreException(ErrorType.RESPONSE_NOT_VISIBLE)
        }

        return response
    }

    private fun releaseIfDue(releaseState: MissionReleaseState?): MissionReleaseState? {
        val scheduledAt = releaseState?.releaseScheduledAt ?: return releaseState
        if (releaseState.releasedAt != null || clock.instant().isBefore(scheduledAt)) {
            return releaseState
        }

        return missionReleaseStateRepository.save(
            releaseState.copy(releasedAt = clock.instant()),
        )
    }
}
