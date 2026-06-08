package com.replus.api.room.interfaces.rest

import com.replus.api.common.interfaces.rest.dto.RoomMemberResponse
import com.replus.api.common.interfaces.rest.dto.RoomTodayResponseStatus
import com.replus.api.common.interfaces.rest.dto.RoomTodaySummaryResponse
import com.replus.api.common.interfaces.rest.dto.UserSummaryResponse
import com.replus.api.common.interfaces.rest.dto.toSummaryResponse
import com.replus.api.mission.application.port.VideoStoragePort
import com.replus.api.room.application.GrowthRewardsResult
import com.replus.api.room.application.InviteLinkResult
import com.replus.api.room.application.RemoveMemberResult
import com.replus.api.room.application.RoomDetailResult
import com.replus.api.room.application.RoomWallResult
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class CreateRoomRequest(
    @field:NotBlank
    @field:Size(max = 32)
    val name: String,
)

data class CreateInviteLinkRequest(
    @field:Min(1)
    @field:Max(720)
    val expiresInHours: Long? = 168,

    @field:Min(1)
    @field:Max(5)
    val maxUses: Int? = null,

    val rotate: Boolean = false,
)

data class RoomDetailResponse(
    val id: UUID,
    val name: String,
    val memberCount: Int,
    val maxMembers: Int,
    val ownerMemberId: UUID,
    val currentUserMemberId: UUID,
    val currentUserRole: String,
    val members: List<RoomMemberResponse>,
    val createdAt: Instant,
    val today: RoomTodaySummaryResponse?,
)

data class InviteLinkResponse(
    val code: String,
    val roomId: UUID,
    val url: String,
    val expiresAt: Instant,
    val maxUses: Int?,
    val uses: Int,
)

data class RemoveMemberResponse(
    val memberId: UUID,
    val status: String,
    val removedAt: Instant,
)

data class GrowthRewardsResponse(
    val roomId: UUID,
    val rewards: List<GrowthRewardResponse>,
)

data class GrowthRewardResponse(
    val id: UUID,
    val roomId: UUID,
    val type: String,
    val category: String,
    val title: String,
    val description: String,
    val status: String,
    val progress: Int,
    val threshold: Int,
    val unlockedAt: Instant?,
    val assetKey: String?,
)

data class RoomWallResponse(
    val roomId: UUID,
    val viewer: WallViewerResponse,
    val viewport: WallViewportResponse,
    val frames: List<WallFrameResponse>,
)

data class WallViewerResponse(
    val memberId: UUID,
    val role: String,
    val hasSubmittedToday: Boolean,
    val todayResponseId: UUID?,
)

data class WallViewportResponse(
    val width: Int,
    val height: Int,
    val minZoom: Double,
    val maxZoom: Double,
)

data class WallFrameResponse(
    val id: UUID,
    val roomId: UUID,
    val missionId: UUID,
    val missionDate: LocalDate,
    val slotIndex: Int,
    val category: String,
    val status: String,
    val position: WallFramePositionResponse,
    val response: WallMissionResponsePreviewResponse?,
)

data class WallFramePositionResponse(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val rotation: Double,
)

data class WallMissionResponsePreviewResponse(
    val id: UUID,
    val missionId: UUID,
    val memberId: UUID,
    val author: UserSummaryResponse,
    val isMine: Boolean,
    val status: String,
    val visibility: String,
    val video: WallVideoAssetResponse?,
    val reactionSummary: List<WallReactionSummaryItemResponse>,
    val createdAt: Instant,
    val deletedAt: Instant?,
)

data class WallVideoAssetResponse(
    val objectKey: String,
    val playbackUrl: String,
    val thumbnailUrl: String?,
    val contentType: String,
    val durationSeconds: Int,
    val hasAudio: Boolean,
    val width: Int?,
    val height: Int?,
    val fileSizeBytes: Long?,
)

data class WallReactionSummaryItemResponse(
    val type: String,
    val count: Int,
    val reactedByViewer: Boolean,
)

fun RoomDetailResult.toResponse(): RoomDetailResponse =
    RoomDetailResponse(
        id = room.id,
        name = room.name,
        memberCount = members.size,
        maxMembers = room.maxMembers,
        ownerMemberId = room.ownerMemberId,
        currentUserMemberId = currentMember.id,
        currentUserRole = currentMember.role.name,
        members = members.map {
            RoomMemberResponse(
                id = it.member.id,
                roomId = it.member.roomId,
                user = UserSummaryResponse(
                    id = it.user.id,
                    displayName = it.user.displayName,
                    avatarUrl = it.user.avatarUrl,
                ),
                role = it.member.role,
                status = it.member.status.name,
                joinedAt = it.member.joinedAt,
                removedAt = it.member.removedAt,
            )
        },
        createdAt = room.createdAt,
        today = today?.let {
            RoomTodaySummaryResponse(
                missionId = it.mission.id,
                missionDate = it.mission.missionDate,
                prompt = it.mission.prompt,
                category = it.mission.category,
                myResponseStatus = if (it.myResponseId == null) {
                    RoomTodayResponseStatus.NOT_SUBMITTED
                } else {
                    RoomTodayResponseStatus.SUBMITTED
                },
                myResponseId = it.myResponseId,
            )
        },
    )

fun InviteLinkResult.toResponse(): InviteLinkResponse =
    InviteLinkResponse(
        code = inviteLink.code,
        roomId = inviteLink.roomId,
        url = url,
        expiresAt = inviteLink.expiresAt,
        maxUses = inviteLink.maxUses,
        uses = inviteLink.uses,
    )

fun RemoveMemberResult.toResponse(): RemoveMemberResponse =
    RemoveMemberResponse(
        memberId = member.id,
        status = member.status.name,
        removedAt = member.removedAt!!,
    )

fun GrowthRewardsResult.toResponse(): GrowthRewardsResponse =
    GrowthRewardsResponse(
        roomId = roomId,
        rewards = rewards.map {
            GrowthRewardResponse(
                id = it.id,
                roomId = it.roomId,
                type = it.type.name,
                category = it.category.name,
                title = it.title,
                description = it.description,
                status = it.status.name,
                progress = it.progress,
                threshold = it.threshold,
                unlockedAt = it.unlockedAt,
                assetKey = it.assetKey,
            )
        },
    )

fun RoomWallResult.toResponse(videoStoragePort: VideoStoragePort): RoomWallResponse =
    RoomWallResponse(
        roomId = roomId,
        viewer = WallViewerResponse(
            memberId = viewer.memberId,
            role = viewer.role.name,
            hasSubmittedToday = viewer.hasSubmittedToday,
            todayResponseId = viewer.todayResponseId,
        ),
        viewport = WallViewportResponse(
            width = viewport.width,
            height = viewport.height,
            minZoom = viewport.minZoom,
            maxZoom = viewport.maxZoom,
        ),
        frames = frames.map { frame ->
            WallFrameResponse(
                id = frame.id,
                roomId = frame.roomId,
                missionId = frame.mission.id,
                missionDate = frame.mission.missionDate,
                slotIndex = frame.slotIndex,
                category = frame.mission.category.name,
                status = frame.status.name,
                position = WallFramePositionResponse(
                    x = frame.position.x,
                    y = frame.position.y,
                    width = frame.position.width,
                    height = frame.position.height,
                    rotation = frame.position.rotation,
                ),
                response = frame.response?.let { preview ->
                    WallMissionResponsePreviewResponse(
                        id = preview.response.id,
                        missionId = preview.response.missionId,
                        memberId = preview.response.memberId,
                        author = UserSummaryResponse(
                            id = preview.author.id,
                            displayName = preview.author.displayName,
                            avatarUrl = preview.author.avatarUrl,
                        ),
                        isMine = preview.isMine,
                        status = preview.response.status.name,
                        visibility = preview.visibility.name,
                        video = WallVideoAssetResponse(
                            objectKey = preview.videoAsset.objectKey,
                            playbackUrl = videoStoragePort.playbackUrl(preview.videoAsset.objectKey),
                            thumbnailUrl = preview.videoAsset.thumbnailObjectKey?.let {
                                videoStoragePort.thumbnailUrl(it)
                            },
                            contentType = preview.videoAsset.contentType,
                            durationSeconds = preview.videoAsset.durationSeconds,
                            hasAudio = preview.videoAsset.hasAudio,
                            width = preview.videoAsset.width,
                            height = preview.videoAsset.height,
                            fileSizeBytes = preview.videoAsset.fileSizeBytes,
                        ),
                        reactionSummary = preview.reactionSummary.map { summary ->
                            WallReactionSummaryItemResponse(
                                type = summary.type.name,
                                count = summary.count,
                                reactedByViewer = summary.reactedByViewer,
                            )
                        },
                        createdAt = preview.response.createdAt,
                        deletedAt = preview.response.deletedAt,
                    )
                },
            )
        },
    )
