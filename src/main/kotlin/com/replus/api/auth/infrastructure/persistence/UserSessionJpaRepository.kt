package com.replus.api.auth.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserSessionJpaRepository : JpaRepository<UserSessionEntity, UUID> {
    fun findByTokenHashAndRevokedAtIsNull(tokenHash: String): UserSessionEntity?
}
