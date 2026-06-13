package com.replus.api.common.security

import java.time.Instant
import java.util.UUID

interface SessionStore {
    fun register(userId: UUID, expiresAt: Instant): String

    fun resolve(token: String, now: Instant): UUID?
}
