package com.replus.api.common.security

import java.util.UUID

data class AuthenticatedUser(
    val userId: UUID,
)
