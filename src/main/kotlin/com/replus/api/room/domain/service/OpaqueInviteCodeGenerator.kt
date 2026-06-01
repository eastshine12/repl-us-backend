package com.replus.api.room.domain.service

import java.security.SecureRandom
import java.util.UUID

class OpaqueInviteCodeGenerator : InviteCodeGenerator {
    private val random = SecureRandom()

    override fun generate(roomId: UUID): String {
        val ignored = roomId
        return buildString(CODE_LENGTH) {
            repeat(CODE_LENGTH) {
                append(ALPHABET[random.nextInt(ALPHABET.length)])
            }
        }.also {
            require(!ignored.toString().contains(it)) {
                "Generated invite code must not expose the room id."
            }
        }
    }

    companion object {
        private const val CODE_LENGTH = 6
        private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }
}
