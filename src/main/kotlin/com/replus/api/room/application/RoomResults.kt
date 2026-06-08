package com.replus.api.room.application

import com.replus.api.auth.domain.model.User
import com.replus.api.mission.domain.model.Mission
import com.replus.api.mission.domain.model.MissionCategory
import com.replus.api.mission.domain.model.MissionResponse
import com.replus.api.mission.domain.model.ReactionType
import com.replus.api.mission.domain.model.VideoAsset
import com.replus.api.room.domain.model.InviteLink
import com.replus.api.room.domain.model.Room
import com.replus.api.room.domain.model.RoomMember
import com.replus.api.room.domain.model.RoomRole
import java.time.Instant
import java.util.UUID

data class RoomMemberResult(
    val member: RoomMember,
    val user: User,
)

data class RoomDetailResult(
    val room: Room,
    val currentMember: RoomMember,
    val members: List<RoomMemberResult>,
    val today: RoomTodaySummaryResult?,
)

data class RoomTodaySummaryResult(
    val mission: Mission,
    val myResponseId: UUID?,
)

data class InviteLinkResult(
    val inviteLink: InviteLink,
    val url: String,
)

data class RemoveMemberResult(
    val member: RoomMember,
)

data class GrowthRewardsResult(
    val roomId: UUID,
    val rewards: List<GrowthRewardResult>,
)

data class GrowthRewardResult(
    val id: UUID,
    val roomId: UUID,
    val type: GrowthRewardType,
    val category: MissionCategory,
    val title: String,
    val description: String,
    val status: GrowthRewardStatus,
    val progress: Int,
    val threshold: Int,
    val unlockedAt: Instant?,
    val assetKey: String?,
)

enum class GrowthRewardType {
    ROOM_NAMEPLATE,
    FRIDGE_MAGNET,
    MONTHLY_FRAME,
}

enum class GrowthRewardStatus {
    LOCKED,
    UNLOCKED,
}

data class RoomWallResult(
    val roomId: UUID,
    val viewer: WallViewerResult,
    val viewport: WallViewportResult,
    val frames: List<WallFrameResult>,
)

data class WallViewerResult(
    val memberId: UUID,
    val role: RoomRole,
    val hasSubmittedToday: Boolean,
    val todayResponseId: UUID?,
)

data class WallViewportResult(
    val width: Int,
    val height: Int,
    val minZoom: Double,
    val maxZoom: Double,
)

data class WallFrameResult(
    val id: UUID,
    val roomId: UUID,
    val mission: Mission,
    val slotIndex: Int,
    val status: WallFrameStatus,
    val position: WallFramePositionResult,
    val response: WallResponsePreviewResult?,
)

data class WallFramePositionResult(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val rotation: Double,
)

data class WallResponsePreviewResult(
    val response: MissionResponse,
    val author: User,
    val isMine: Boolean,
    val visibility: WallResponseVisibility,
    val videoAsset: VideoAsset,
    val reactionSummary: List<WallReactionSummaryResult>,
)

data class WallReactionSummaryResult(
    val type: ReactionType,
    val count: Int,
    val reactedByViewer: Boolean,
)

enum class WallFrameStatus {
    READY,
    LOCKED,
}

enum class WallResponseVisibility {
    VISIBLE,
}
