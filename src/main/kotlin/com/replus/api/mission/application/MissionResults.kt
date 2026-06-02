package com.replus.api.mission.application

import com.replus.api.auth.domain.model.User
import com.replus.api.mission.domain.model.Mission
import com.replus.api.mission.domain.model.MissionResponse
import com.replus.api.mission.domain.model.MissionReleaseState
import com.replus.api.mission.domain.model.ReactionType
import com.replus.api.mission.domain.model.VideoAsset
import com.replus.api.room.domain.model.Room
import com.replus.api.room.domain.model.RoomMember
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class TodayResult(
    val serverDate: LocalDate,
    val room: Room,
    val viewer: ViewerStateResult,
    val currentMember: RoomMember,
    val members: List<TodayMemberResult>,
    val memberCount: Int,
    val mission: Mission,
    val participation: ParticipationResult,
    val responses: List<TodayMissionResponseResult>,
    val releaseState: MissionReleaseState?,
)

data class TodayMissionResponseResult(
    val response: MissionResponse,
    val videoAsset: VideoAsset,
    val reactionSummary: List<ReactionSummaryResult>,
)

data class ReactionSummaryResult(
    val type: ReactionType,
    val count: Int,
    val reactedByViewer: Boolean,
)

data class TodayMemberResult(
    val member: RoomMember,
    val user: User,
)

data class ViewerStateResult(
    val memberId: UUID,
    val role: String,
    val hasSubmittedToday: Boolean,
    val todayResponseId: UUID?,
)

data class ParticipationResult(
    val totalActiveMembers: Int,
    val submittedCount: Int,
    val viewerHasSubmitted: Boolean,
    val canViewFriendResponses: Boolean,
    val allSubmitted: Boolean,
)

data class MissionResponseUploadMetadata(
    val contentType: String,
    val fileSizeBytes: Long,
    val durationSeconds: Int,
    val hasAudio: Boolean,
    val width: Int?,
    val height: Int?,
)

data class MissionResponseCreateCommand(
    val objectKey: String,
    val contentType: String,
    val fileSizeBytes: Long,
    val durationSeconds: Int,
    val hasAudio: Boolean,
    val width: Int?,
    val height: Int?,
    val clientCapturedAt: Instant?,
)

data class MissionResponseUploadUrlResult(
    val uploadUrl: String,
    val method: String,
    val objectKey: String,
    val requiredHeaders: Map<String, String>,
    val expiresAt: Instant,
    val maxFileSizeBytes: Long,
)

data class CreatedMissionResponseResult(
    val response: MissionResponse,
    val videoAsset: VideoAsset,
    val author: User,
)
