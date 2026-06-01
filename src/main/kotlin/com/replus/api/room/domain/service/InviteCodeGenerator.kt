package com.replus.api.room.domain.service

import java.util.UUID

interface InviteCodeGenerator {
    fun generate(roomId: UUID): String
}
