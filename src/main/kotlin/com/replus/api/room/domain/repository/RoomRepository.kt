package com.replus.api.room.domain.repository

import com.replus.api.room.domain.model.Room
import java.util.UUID

interface RoomRepository {
    fun getById(roomId: UUID): Room

    fun save(room: Room): Room
}
