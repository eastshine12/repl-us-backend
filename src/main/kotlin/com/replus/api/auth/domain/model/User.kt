package com.replus.api.auth.domain.model

import java.time.Instant
import java.util.UUID

data class User(
    val id: UUID,
    val displayName: String,
    val avatarUrl: String?,
    val isGuest: Boolean,
    val createdAt: Instant,
)
