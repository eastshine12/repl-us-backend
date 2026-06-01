package com.replus.api.mission.application

import com.replus.api.auth.domain.model.User
import com.replus.api.mission.domain.model.Mission
import com.replus.api.mission.domain.model.MissionResponse
import com.replus.api.mission.domain.model.MissionReleaseState
import com.replus.api.room.domain.model.Room
import com.replus.api.room.domain.model.RoomMember
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
    val responses: List<MissionResponse>,
    val releaseState: MissionReleaseState?,
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
