package com.replus.api.room.domain.model

import java.time.Instant
import java.util.UUID

data class Room(
    val id: UUID,
    val name: String,
    val ownerMemberId: UUID,
    val maxMembers: Int = MAX_MEMBERS,
    val createdAt: Instant,
) {
    init {
        require(name.isNotBlank()) { "Room name must not be blank." }
        require(name.length <= 32) { "Room name must be 32 characters or fewer." }
        require(maxMembers == MAX_MEMBERS) { "MVP rooms must have exactly $MAX_MEMBERS seats." }
    }

    companion object {
        const val MAX_MEMBERS = 6
    }
}
