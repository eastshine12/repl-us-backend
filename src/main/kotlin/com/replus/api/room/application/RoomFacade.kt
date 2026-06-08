package com.replus.api.room.application

import com.replus.api.auth.domain.repository.UserRepository
import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType
import com.replus.api.mission.domain.model.MissionCategory
import com.replus.api.mission.domain.model.MissionReleaseState
import com.replus.api.mission.domain.model.MissionResponseStatus
import com.replus.api.mission.domain.model.ReactionType
import com.replus.api.mission.domain.model.ResponseReaction
import com.replus.api.mission.domain.repository.MissionReleaseStateRepository
import com.replus.api.mission.domain.repository.MissionRepository
import com.replus.api.mission.domain.repository.MissionResponseRepository
import com.replus.api.mission.domain.repository.ResponseReactionRepository
import com.replus.api.mission.domain.repository.VideoAssetRepository
import com.replus.api.room.domain.model.InviteLink
import com.replus.api.room.domain.model.Room
import com.replus.api.room.domain.model.RoomMember
import com.replus.api.room.domain.model.RoomMemberStatus
import com.replus.api.room.domain.model.RoomRole
import com.replus.api.room.domain.policy.RoomAccessPolicy
import com.replus.api.room.domain.policy.RoomCapacityPolicy
import com.replus.api.room.domain.repository.InviteLinkRepository
import com.replus.api.room.domain.repository.RoomMemberRepository
import com.replus.api.room.domain.repository.RoomRepository
import com.replus.api.room.domain.service.InviteCodeGenerator
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.UUID

@Service
class RoomFacade(
    private val userRepository: UserRepository,
    private val roomRepository: RoomRepository,
    private val roomMemberRepository: RoomMemberRepository,
    private val inviteLinkRepository: InviteLinkRepository,
    private val missionRepository: MissionRepository,
    private val missionResponseRepository: MissionResponseRepository,
    private val missionReleaseStateRepository: MissionReleaseStateRepository,
    private val videoAssetRepository: VideoAssetRepository,
    private val responseReactionRepository: ResponseReactionRepository,
    private val roomAccessPolicy: RoomAccessPolicy,
    private val roomCapacityPolicy: RoomCapacityPolicy,
    private val inviteCodeGenerator: InviteCodeGenerator,
    private val clock: Clock,
    @Value("\${replus.web-base-url:http://localhost:3000}")
    private val webBaseUrl: String,
) {
    @Transactional
    fun createRoom(userId: UUID, name: String): RoomDetailResult {
        val now = clock.instant()
        val roomId = UUID.randomUUID()
        val ownerMemberId = UUID.randomUUID()
        val room = roomRepository.save(
            Room(
                id = roomId,
                name = name.trim(),
                ownerMemberId = ownerMemberId,
                maxMembers = Room.MAX_MEMBERS,
                createdAt = now,
            ),
        )
        roomMemberRepository.save(
            RoomMember(
                id = ownerMemberId,
                roomId = roomId,
                userId = userId,
                role = RoomRole.OWNER,
                status = RoomMemberStatus.ACTIVE,
                slotIndex = 0,
                joinedAt = now,
                removedAt = null,
            ),
        )
        return getRoomDetail(userId, room.id)
    }

    @Transactional(readOnly = true)
    fun getRoomDetail(userId: UUID, roomId: UUID): RoomDetailResult {
        val room = roomRepository.getById(roomId)
        val currentMember = roomMemberRepository.findActiveByRoomIdAndUserId(roomId, userId)
        roomAccessPolicy.requireActiveMember(currentMember)

        val members = roomMemberRepository.findActiveByRoomId(roomId).map { member ->
            RoomMemberResult(
                member = member,
                user = userRepository.getById(member.userId),
            )
        }
        val todayMission = missionRepository.findByRoomIdAndMissionDate(roomId, today())
        val myTodayResponse = todayMission?.let {
            missionResponseRepository.findActiveByMissionIdAndMemberId(it.id, currentMember!!.id)
        }
        return RoomDetailResult(
            room = room,
            currentMember = currentMember!!,
            members = members,
            today = todayMission?.let {
                RoomTodaySummaryResult(
                    mission = it,
                    myResponseId = myTodayResponse?.id,
                )
            },
        )
    }

    @Transactional
    fun createInviteLink(
        userId: UUID,
        roomId: UUID,
        expiresInHours: Long,
        maxUses: Int?,
        rotate: Boolean,
    ): InviteLinkResult {
        val now = clock.instant()
        val member = roomMemberRepository.findActiveByRoomIdAndUserId(roomId, userId)
        roomAccessPolicy.requireActiveMember(member)

        val existing = inviteLinkRepository.findLatestUsableByRoomId(roomId, now)
        if (existing != null && !rotate) {
            return existing.toResult()
        }
        if (existing != null) {
            inviteLinkRepository.save(existing.expire(now))
        }

        val inviteLink = inviteLinkRepository.save(
            InviteLink(
                code = generateUniqueInviteCode(roomId),
                roomId = roomId,
                createdByMemberId = member!!.id,
                expiresAt = now.plus(Duration.ofHours(expiresInHours)),
                maxUses = maxUses,
                uses = 0,
                createdAt = now,
            ),
        )
        return inviteLink.toResult()
    }

    @Transactional
    fun joinByInviteCode(userId: UUID, code: String): RoomDetailResult {
        val now = clock.instant()
        val inviteCode = code.toValidatedInviteCode()
        val inviteLink = inviteLinkRepository.findByCode(inviteCode)
            ?: throw CoreException(ErrorType.INVITE_LINK_NOT_FOUND)
        if (inviteLink.isExpired(now)) {
            throw CoreException(ErrorType.INVITE_LINK_EXPIRED)
        }
        if (inviteLink.hasReachedUsageLimit()) {
            throw CoreException(ErrorType.INVITE_LINK_USAGE_LIMIT_REACHED)
        }

        val room = roomRepository.getById(inviteLink.roomId)
        val existingMember = roomMemberRepository.findByRoomIdAndUserId(room.id, userId)
        if (existingMember?.isActive() == true) {
            return getRoomDetail(userId, room.id)
        }

        roomCapacityPolicy.ensureCanJoin(
            activeMemberCount = roomMemberRepository.countActiveByRoomId(room.id),
            maxMembers = room.maxMembers,
        )

        val member = if (existingMember != null) {
            existingMember.copy(
                role = RoomRole.MEMBER,
                status = RoomMemberStatus.ACTIVE,
                slotIndex = roomMemberRepository.nextSlotIndex(room.id),
                joinedAt = now,
                removedAt = null,
            )
        } else {
            RoomMember(
                id = UUID.randomUUID(),
                roomId = room.id,
                userId = userId,
                role = RoomRole.MEMBER,
                status = RoomMemberStatus.ACTIVE,
                slotIndex = roomMemberRepository.nextSlotIndex(room.id),
                joinedAt = now,
                removedAt = null,
            )
        }

        roomMemberRepository.save(member)
        inviteLinkRepository.save(inviteLink.recordUse())
        return getRoomDetail(userId, room.id)
    }

    @Transactional
    fun removeMember(userId: UUID, roomId: UUID, memberId: UUID): RemoveMemberResult {
        val now = clock.instant()
        roomRepository.getById(roomId)

        val actor = roomMemberRepository.findActiveByRoomIdAndUserId(roomId, userId)
        roomAccessPolicy.requireActiveMember(actor)
        roomAccessPolicy.requireOwner(actor!!)

        val target = roomMemberRepository.findActiveByIdAndRoomId(memberId, roomId)
            ?: throw CoreException(ErrorType.RESOURCE_NOT_FOUND)
        if (target.isOwner()) {
            throw CoreException(ErrorType.CANNOT_REMOVE_OWNER)
        }

        return RemoveMemberResult(
            member = roomMemberRepository.save(target.remove(now)),
        )
    }

    @Transactional
    fun leaveRoom(userId: UUID, roomId: UUID): RemoveMemberResult {
        val now = clock.instant()
        roomRepository.getById(roomId)

        val member = roomMemberRepository.findActiveByRoomIdAndUserId(roomId, userId)
        roomAccessPolicy.requireActiveMember(member)
        if (member!!.isOwner()) {
            throw CoreException(ErrorType.CANNOT_REMOVE_OWNER)
        }

        return RemoveMemberResult(
            member = roomMemberRepository.save(member.remove(now)),
        )
    }

    @Transactional(readOnly = true)
    fun getGrowthRewards(userId: UUID, roomId: UUID): GrowthRewardsResult {
        roomRepository.getById(roomId)
        val member = roomMemberRepository.findActiveByRoomIdAndUserId(roomId, userId)
        roomAccessPolicy.requireActiveMember(member)

        val activeResponseCount = missionResponseRepository.countActiveByRoomId(roomId)
        return GrowthRewardsResult(
            roomId = roomId,
            rewards = growthRewardDefinitions.map { definition ->
                val status = if (activeResponseCount >= definition.threshold) {
                    GrowthRewardStatus.UNLOCKED
                } else {
                    GrowthRewardStatus.LOCKED
                }
                GrowthRewardResult(
                    id = rewardId(roomId, definition.type),
                    roomId = roomId,
                    type = definition.type,
                    category = definition.category,
                    title = definition.title,
                    description = definition.description,
                    status = status,
                    progress = activeResponseCount,
                    threshold = definition.threshold,
                    unlockedAt = null,
                    assetKey = definition.assetKey,
                )
            },
        )
    }

    @Transactional
    fun getRoomWall(
        userId: UUID,
        roomId: UUID,
        from: LocalDate?,
        to: LocalDate?,
    ): RoomWallResult {
        roomRepository.getById(roomId)
        val currentMember = roomMemberRepository.findActiveByRoomIdAndUserId(roomId, userId)
        roomAccessPolicy.requireActiveMember(currentMember)
        validateWallDateRange(from, to)

        val missions = if (from == null && to == null) {
            missionRepository.findAllByRoomId(roomId)
        } else {
            missionRepository.findAllByRoomIdAndMissionDateBetween(
                roomId = roomId,
                from = from ?: MIN_WALL_DATE,
                to = to ?: MAX_WALL_DATE,
            )
        }
        val missionIds = missions.map { it.id }
        val responses = missionResponseRepository.findAllByMissionIds(missionIds)
        val activeMembers = roomMemberRepository.findActiveByRoomId(roomId).sortedBy { it.slotIndex }
        val membersById = activeMembers.associateBy { it.id }
        val usersByMemberId = membersById.mapValues { userRepository.getById(it.value.userId) }
        val videoAssetsById = videoAssetRepository
            .findAllByIds(responses.filter { it.isActive() }.map { it.videoAssetId })
            .associateBy { it.id }
        val reactionsByResponseId = responseReactionRepository
            .findAllByResponseIds(responses.filter { it.isActive() }.map { it.id })
            .groupBy { it.responseId }
        val releaseStatesByMissionId = missionReleaseStateRepository
            .findAllByMissionIds(missionIds)
            .associateBy { it.missionId }
            .mapValues { releaseIfDue(it.value) }
        val responsesByMissionAndMember = responses.associateBy { it.missionId to it.memberId }
        val todayMission = missionRepository.findByRoomIdAndMissionDate(roomId, today())
        val todayResponse = todayMission?.let {
            missionResponseRepository.findActiveByMissionIdAndMemberId(it.id, currentMember!!.id)
        }

        return RoomWallResult(
            roomId = roomId,
            viewer = WallViewerResult(
                memberId = currentMember!!.id,
                role = currentMember.role,
                hasSubmittedToday = todayResponse != null,
                todayResponseId = todayResponse?.id,
            ),
            viewport = WallViewportResult(width = 1600, height = 1200, minZoom = 0.45, maxZoom = 2.4),
            frames = missions
                .flatMapIndexed { missionIndex, mission ->
                    activeMembers.map { responseMember ->
                        val response = responsesByMissionAndMember[mission.id to responseMember.id]
                        val isMine = responseMember.id == currentMember.id
                        val canView = isMine || releaseStatesByMissionId[mission.id]?.releasedAt?.let {
                            !clock.instant().isBefore(it)
                        } == true
                        val status = when {
                            response == null -> WallFrameStatus.EMPTY
                            response.status == MissionResponseStatus.DELETED -> WallFrameStatus.DELETED
                            canView -> WallFrameStatus.READY
                            else -> WallFrameStatus.LOCKED
                        }
                        WallFrameResult(
                            id = frameId(mission.id, responseMember.id),
                            roomId = roomId,
                            mission = mission,
                            slotIndex = responseMember.slotIndex,
                            status = status,
                            position = framePosition(
                                missionIndex = missionIndex,
                                slotIndex = responseMember.slotIndex,
                            ),
                            response = when (status) {
                                WallFrameStatus.READY -> response?.let {
                                    WallResponsePreviewResult(
                                        response = it,
                                        author = usersByMemberId.getValue(responseMember.id),
                                        isMine = isMine,
                                        visibility = WallResponseVisibility.VISIBLE,
                                        videoAsset = videoAssetsById.getValue(it.videoAssetId),
                                        reactionSummary = reactionSummary(
                                            responseId = it.id,
                                            reactionsByResponseId = reactionsByResponseId,
                                            viewerMemberId = currentMember.id,
                                        ),
                                    )
                                }
                                WallFrameStatus.DELETED -> response?.let {
                                    WallResponsePreviewResult(
                                        response = it,
                                        author = usersByMemberId.getValue(responseMember.id),
                                        isMine = isMine,
                                        visibility = WallResponseVisibility.VISIBLE,
                                        videoAsset = null,
                                        reactionSummary = emptyList(),
                                    )
                                }
                                WallFrameStatus.EMPTY,
                                WallFrameStatus.LOCKED,
                                -> null
                            },
                        )
                    }
                }
                .sortedWith(
                    compareByDescending<WallFrameResult> { it.mission.missionDate }
                        .thenBy { it.slotIndex },
                ),
        )
    }

    private fun generateUniqueInviteCode(roomId: UUID): String {
        repeat(10) {
            val code = inviteCodeGenerator.generate(roomId)
            if (inviteLinkRepository.findByCode(code) == null) {
                return code
            }
        }
        throw CoreException(ErrorType.INTERNAL_ERROR)
    }

    private fun String.toValidatedInviteCode(): String =
        trim().uppercase(Locale.ROOT).also {
            if (!INVITE_CODE_PATTERN.matches(it)) {
                throw CoreException(ErrorType.INVALID_REQUEST, "Invite code format is invalid.")
            }
        }

    private fun today(): LocalDate =
        LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul")))

    private fun rewardId(roomId: UUID, type: GrowthRewardType): UUID =
        UUID.nameUUIDFromBytes("growth-reward:$roomId:$type".toByteArray(StandardCharsets.UTF_8))

    private fun frameId(missionId: UUID, memberId: UUID): UUID =
        UUID.nameUUIDFromBytes("wall-frame:$missionId:$memberId".toByteArray(StandardCharsets.UTF_8))

    private fun framePosition(missionIndex: Int, slotIndex: Int): WallFramePositionResult =
        WallFramePositionResult(
            x = 120.0 + (slotIndex % 3) * 260.0,
            y = 100.0 + missionIndex.coerceAtLeast(0) * 220.0,
            width = 220.0,
            height = 160.0,
            rotation = (slotIndex % 5 - 2) * 1.5,
        )

    private fun validateWallDateRange(from: LocalDate?, to: LocalDate?) {
        if (from != null && to != null && from.isAfter(to)) {
            throw CoreException(ErrorType.INVALID_REQUEST, "from must be on or before to.")
        }
    }

    private fun releaseIfDue(releaseState: MissionReleaseState): MissionReleaseState {
        val scheduledAt = releaseState.releaseScheduledAt ?: return releaseState
        if (releaseState.releasedAt != null || clock.instant().isBefore(scheduledAt)) {
            return releaseState
        }
        return missionReleaseStateRepository.save(releaseState.copy(releasedAt = clock.instant()))
    }

    private fun reactionSummary(
        responseId: UUID,
        reactionsByResponseId: Map<UUID, List<ResponseReaction>>,
        viewerMemberId: UUID,
    ): List<WallReactionSummaryResult> =
        ReactionType.entries.mapNotNull { type ->
            val reactions = reactionsByResponseId[responseId].orEmpty().filter { it.type == type }
            if (reactions.isEmpty()) {
                null
            } else {
                WallReactionSummaryResult(
                    type = type,
                    count = reactions.size,
                    reactedByViewer = reactions.any { it.memberId == viewerMemberId },
                )
            }
        }

    private fun InviteLink.toResult(): InviteLinkResult =
        InviteLinkResult(
            inviteLink = this,
            url = "${webBaseUrl.trimEnd('/')}/join/$code",
        )

    private companion object {
        private val INVITE_CODE_PATTERN = Regex("^[A-HJ-NP-Z2-9]{6,32}$")
        private val MIN_WALL_DATE: LocalDate = LocalDate.of(1970, 1, 1)
        private val MAX_WALL_DATE: LocalDate = LocalDate.of(2100, 12, 31)
        private val growthRewardDefinitions = listOf(
            GrowthRewardDefinition(
                type = GrowthRewardType.ROOM_NAMEPLATE,
                category = MissionCategory.OBSERVATION,
                title = "Room Nameplate",
                description = "Submit the room's first 3-second response.",
                threshold = 1,
                assetKey = "growth/room-nameplate",
            ),
            GrowthRewardDefinition(
                type = GrowthRewardType.FRIDGE_MAGNET,
                category = MissionCategory.MOOD,
                title = "Fridge Magnet",
                description = "Collect three active responses in the room.",
                threshold = 3,
                assetKey = "growth/fridge-magnet",
            ),
            GrowthRewardDefinition(
                type = GrowthRewardType.MONTHLY_FRAME,
                category = MissionCategory.FULL_PARTICIPATION,
                title = "Monthly Frame",
                description = "Keep the room alive with seven active responses.",
                threshold = 7,
                assetKey = "growth/monthly-frame",
            ),
        )
    }

    private data class GrowthRewardDefinition(
        val type: GrowthRewardType,
        val category: MissionCategory,
        val title: String,
        val description: String,
        val threshold: Int,
        val assetKey: String,
    )
}
