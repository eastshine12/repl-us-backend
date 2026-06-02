package com.replus.api.mission.application

import com.replus.api.auth.domain.repository.UserRepository
import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType
import com.replus.api.mission.application.port.VideoStoragePort
import com.replus.api.mission.domain.model.Mission
import com.replus.api.mission.domain.model.MissionCategory
import com.replus.api.mission.domain.model.MissionReleaseState
import com.replus.api.mission.domain.model.MissionResponse
import com.replus.api.mission.domain.model.MissionResponseStatus
import com.replus.api.mission.domain.model.ReactionType
import com.replus.api.mission.domain.model.ResponseReaction
import com.replus.api.mission.domain.model.VideoAsset
import com.replus.api.mission.domain.policy.MissionEditPolicy
import com.replus.api.mission.domain.policy.MissionResponseSubmissionPolicy
import com.replus.api.mission.domain.repository.MissionReleaseStateRepository
import com.replus.api.mission.domain.repository.MissionRepository
import com.replus.api.mission.domain.repository.MissionResponseRepository
import com.replus.api.mission.domain.repository.ResponseReactionRepository
import com.replus.api.mission.domain.repository.VideoAssetRepository
import com.replus.api.room.domain.policy.RoomAccessPolicy
import com.replus.api.room.domain.repository.RoomMemberRepository
import com.replus.api.room.domain.repository.RoomRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.Instant
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
    private val videoAssetRepository: VideoAssetRepository,
    private val responseReactionRepository: ResponseReactionRepository,
    private val videoStoragePort: VideoStoragePort,
    private val roomAccessPolicy: RoomAccessPolicy,
    private val missionEditPolicy: MissionEditPolicy,
    private val missionResponseSubmissionPolicy: MissionResponseSubmissionPolicy,
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
        val releaseState = releaseIfDue(missionReleaseStateRepository.findByMissionId(mission.id))
        val allSubmitted = responses.size == activeMembers.size && activeMembers.isNotEmpty()
        val canViewFriendResponses = releaseState?.releasedAt?.let { !clock.instant().isBefore(it) } ?: false
        val videoAssetsById = videoAssetRepository
            .findAllByIds(responses.map { it.videoAssetId })
            .associateBy { it.id }
        val reactionsByResponseId = responseReactionRepository
            .findAllByResponseIds(responses.map { it.id })
            .groupBy { it.responseId }

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
            responses = responses.map { response ->
                TodayMissionResponseResult(
                    response = response,
                    videoAsset = videoAssetsById.getValue(response.videoAssetId),
                    reactionSummary = reactionSummary(
                        reactions = reactionsByResponseId[response.id] ?: emptyList(),
                        viewerMemberId = currentMember!!.id,
                    ),
                )
            },
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

    @Transactional(readOnly = true)
    fun createResponseUploadUrl(
        userId: UUID,
        roomId: UUID,
        missionId: UUID,
        metadata: MissionResponseUploadMetadata,
    ): MissionResponseUploadUrlResult {
        val member = requireActiveMember(userId, roomId)
        val mission = requireTodayMission(missionId, roomId)
        missionResponseSubmissionPolicy.validateVideoMetadata(metadata.durationSeconds, metadata.hasAudio)
        missionResponseSubmissionPolicy.ensureCanCreate(
            missionResponseRepository.findActiveByMissionIdAndMemberId(mission.id, member.id),
        )

        val objectKey = objectKey(
            roomId = roomId,
            missionId = mission.id,
            memberId = member.id,
            contentType = metadata.contentType,
        )
        val uploadTarget = videoStoragePort.createUploadTarget(
            objectKey = objectKey,
            contentType = metadata.contentType,
            expiresAt = clock.instant().plus(UPLOAD_URL_TTL),
            maxFileSizeBytes = MAX_UPLOAD_FILE_SIZE_BYTES,
        )

        return MissionResponseUploadUrlResult(
            uploadUrl = uploadTarget.uploadUrl,
            method = uploadTarget.method,
            objectKey = uploadTarget.objectKey,
            requiredHeaders = uploadTarget.requiredHeaders,
            expiresAt = uploadTarget.expiresAt,
            maxFileSizeBytes = uploadTarget.maxFileSizeBytes,
        )
    }

    @Transactional
    fun createMissionResponse(
        userId: UUID,
        roomId: UUID,
        missionId: UUID,
        command: MissionResponseCreateCommand,
    ): CreatedMissionResponseResult {
        val member = requireActiveMember(userId, roomId)
        val mission = requireTodayMission(missionId, roomId)
        missionResponseSubmissionPolicy.validateVideoMetadata(command.durationSeconds, command.hasAudio)
        missionResponseSubmissionPolicy.ensureCanCreate(
            missionResponseRepository.findActiveByMissionIdAndMemberId(mission.id, member.id),
        )

        val expectedObjectKey = objectKey(
            roomId = roomId,
            missionId = mission.id,
            memberId = member.id,
            contentType = command.contentType,
        )
        if (command.objectKey != expectedObjectKey) {
            throw CoreException(ErrorType.INVALID_REQUEST)
        }

        val now = clock.instant()
        val videoAsset = videoAssetRepository.save(
            VideoAsset(
                id = UUID.randomUUID(),
                objectKey = command.objectKey,
                contentType = command.contentType,
                fileSizeBytes = command.fileSizeBytes,
                durationSeconds = command.durationSeconds,
                hasAudio = command.hasAudio,
                width = command.width,
                height = command.height,
                thumbnailObjectKey = null,
                createdAt = now,
            ),
        )
        val response = missionResponseRepository.save(
            MissionResponse(
                id = UUID.randomUUID(),
                roomId = roomId,
                missionId = mission.id,
                memberId = member.id,
                videoAssetId = videoAsset.id,
                status = MissionResponseStatus.ACTIVE,
                createdAt = now,
                deletedAt = null,
            ),
        )
        scheduleReleaseIfAllSubmitted(mission.id, roomId, now)

        return CreatedMissionResponseResult(
            response = response,
            videoAsset = videoAsset,
            author = userRepository.getById(member.userId),
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

    private fun requireActiveMember(userId: UUID, roomId: UUID) =
        roomMemberRepository.findActiveByRoomIdAndUserId(roomId, userId)
            .also { roomAccessPolicy.requireActiveMember(it) }!!

    private fun requireTodayMission(missionId: UUID, roomId: UUID): Mission {
        val mission = missionRepository.getByIdAndRoomId(missionId, roomId)
        if (mission.missionDate != today()) {
            throw CoreException(ErrorType.RESOURCE_NOT_FOUND)
        }
        return mission
    }

    private fun objectKey(roomId: UUID, missionId: UUID, memberId: UUID, contentType: String): String =
        "rooms/$roomId/missions/$missionId/members/$memberId.${extension(contentType)}"

    private fun scheduleReleaseIfAllSubmitted(missionId: UUID, roomId: UUID, now: Instant) {
        if (missionReleaseStateRepository.findByMissionId(missionId) != null) {
            return
        }

        val activeMemberCount = roomMemberRepository.countActiveByRoomId(roomId)
        val submittedCount = missionResponseRepository.countActiveByMissionId(missionId)
        if (activeMemberCount > 0 && submittedCount >= activeMemberCount) {
            missionReleaseStateRepository.save(
                MissionReleaseState(
                    missionId = missionId,
                    allSubmittedAt = now,
                    releaseScheduledAt = now.plus(RELEASE_DELAY),
                    releasedAt = null,
                    failedAt = null,
                ),
            )
        }
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

    private fun reactionSummary(
        reactions: List<ResponseReaction>,
        viewerMemberId: UUID,
    ): List<ReactionSummaryResult> =
        ReactionType.entries.mapNotNull { type ->
            val reactionsOfType = reactions.filter { it.type == type }
            if (reactionsOfType.isEmpty()) {
                null
            } else {
                ReactionSummaryResult(
                    type = type,
                    count = reactionsOfType.size,
                    reactedByViewer = reactionsOfType.any { it.memberId == viewerMemberId },
                )
            }
        }

    private fun extension(contentType: String): String =
        when (contentType.lowercase()) {
            "video/webm" -> "webm"
            "video/mp4" -> "mp4"
            "video/quicktime" -> "mov"
            else -> throw CoreException(ErrorType.INVALID_REQUEST)
        }

    companion object {
        const val MAX_UPLOAD_FILE_SIZE_BYTES: Long = 15_000_000
        private val UPLOAD_URL_TTL: Duration = Duration.ofMinutes(10)
        private val RELEASE_DELAY: Duration = Duration.ofSeconds(60)
    }
}
