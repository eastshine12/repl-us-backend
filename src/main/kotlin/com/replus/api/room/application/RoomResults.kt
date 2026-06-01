package com.replus.api.room.application

import com.replus.api.auth.domain.model.User
import com.replus.api.room.domain.model.InviteLink
import com.replus.api.room.domain.model.Room
import com.replus.api.room.domain.model.RoomMember

data class RoomMemberResult(
    val member: RoomMember,
    val user: User,
)

data class RoomDetailResult(
    val room: Room,
    val currentMember: RoomMember,
    val members: List<RoomMemberResult>,
)

data class InviteLinkResult(
    val inviteLink: InviteLink,
    val url: String,
)
