package com.replus.api.mission.interfaces.rest

import com.replus.api.mission.application.TodayResult
import com.replus.api.mission.domain.model.Mission
import com.replus.api.mission.domain.model.MissionCategory
import com.replus.api.common.interfaces.rest.dto.UserSummaryResponse
import com.replus.api.room.domain.model.RoomRole
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class UpdateMissionRequest(
    @field:NotBlank
    @field:Size(max = 80)
    val prompt: String,
    val category: MissionCategory,
)

data class MissionResponseDto(
    val id: UUID,
    val roomId: UUID,
    val missionDate: LocalDate,
    val prompt: String,
    val category: MissionCategory,
    val editCount: Int,
    val canEdit: Boolean,
    val editedByMemberId: UUID?,
    val editedAt: Instant?,
    val createdAt: Instant,
)

data class ViewerStateResponse(
    val memberId: UUID,
    val role: RoomRole,
    val hasSubmittedToday: Boolean,
    val todayResponseId: UUID?,
)

data class ParticipationSummaryResponse(
    val totalActiveMembers: Int,
    val submittedCount: Int,
    val viewerHasSubmitted: Boolean,
    val canViewFriendResponses: Boolean,
    val allSubmitted: Boolean,
)

data class TodayResponse(
    val serverDate: LocalDate,
    val room: TodayRoomSummaryResponse,
    val mission: MissionResponseDto,
    val viewer: ViewerStateResponse,
    val participation: ParticipationSummaryResponse,
    val responses: List<MissionResponsePreviewResponse>,
    val todayFrames: List<WallFrameResponse>,
    val growthPreview: List<GrowthRewardResponse>,
)

data class TodayRoomSummaryResponse(
    val id: UUID,
    val name: String,
    val memberCount: Int,
    val maxMembers: Int,
    val currentUserRole: RoomRole,
    val lastMissionDate: LocalDate?,
)

data class MissionResponsePreviewResponse(
    val id: UUID,
    val missionId: UUID,
    val memberId: UUID,
    val author: UserSummaryResponse,
    val isMine: Boolean,
    val status: String,
    val visibility: String,
    val video: VideoAssetResponse?,
    val reactionSummary: List<ReactionSummaryItemResponse>,
    val createdAt: Instant,
    val deletedAt: Instant?,
)

data class VideoAssetResponse(
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

data class ReactionSummaryItemResponse(
    val type: String,
    val count: Int,
    val reactedByViewer: Boolean,
)

data class WallFrameResponse(
    val id: UUID,
)

data class GrowthRewardResponse(
    val id: UUID,
)

fun Mission.toResponse(canEdit: Boolean): MissionResponseDto =
    MissionResponseDto(
        id = id,
        roomId = roomId,
        missionDate = missionDate,
        prompt = prompt,
        category = category,
        editCount = editCount,
        canEdit = canEdit,
        editedByMemberId = editedByMemberId,
        editedAt = editedAt,
        createdAt = createdAt,
    )

fun TodayResult.toResponse(): TodayResponse {
    val canEdit = currentMember.role == RoomRole.OWNER &&
        mission.editCount == 0 &&
        participation.submittedCount == 0

    return TodayResponse(
        serverDate = serverDate,
        room = TodayRoomSummaryResponse(
            id = room.id,
            name = room.name,
            memberCount = memberCount,
            maxMembers = room.maxMembers,
            currentUserRole = currentMember.role,
            lastMissionDate = mission.missionDate,
        ),
        mission = mission.toResponse(canEdit),
        viewer = ViewerStateResponse(
            memberId = viewer.memberId,
            role = currentMember.role,
            hasSubmittedToday = viewer.hasSubmittedToday,
            todayResponseId = viewer.todayResponseId,
        ),
        participation = ParticipationSummaryResponse(
            totalActiveMembers = participation.totalActiveMembers,
            submittedCount = participation.submittedCount,
            viewerHasSubmitted = participation.viewerHasSubmitted,
            canViewFriendResponses = participation.canViewFriendResponses,
            allSubmitted = participation.allSubmitted,
        ),
        responses = responses.map {
            val author = members.first { member -> member.member.id == it.memberId }.user
            MissionResponsePreviewResponse(
                id = it.id,
                missionId = it.missionId,
                memberId = it.memberId,
                author = UserSummaryResponse(
                    id = author.id,
                    displayName = author.displayName,
                    avatarUrl = author.avatarUrl,
                ),
                isMine = it.memberId == currentMember.id,
                status = it.status.name,
                visibility = if (it.memberId == currentMember.id) "VISIBLE" else "LOCKED_UNTIL_VIEWER_SUBMITS",
                video = null,
                reactionSummary = emptyList(),
                createdAt = it.createdAt,
                deletedAt = it.deletedAt,
            )
        },
        todayFrames = emptyList(),
        growthPreview = emptyList(),
    )
}
