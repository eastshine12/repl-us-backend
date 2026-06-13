package com.replus.api.auth.infrastructure.persistence

import com.replus.api.auth.domain.model.AuthProvider
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AuthProviderAccountJpaRepository : JpaRepository<AuthProviderAccountEntity, UUID> {
    fun findByProviderAndProviderSubject(
        provider: AuthProvider,
        providerSubject: String,
    ): AuthProviderAccountEntity?
}
