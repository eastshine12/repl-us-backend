package com.replus.api.room.application

import com.replus.api.auth.domain.model.User
import com.replus.api.mission.domain.model.Mission
import com.replus.api.mission.domain.model.MissionCategory
import com.replus.api.room.domain.model.InviteLink
import com.replus.api.room.domain.model.Room
import com.replus.api.room.domain.model.RoomMember
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
