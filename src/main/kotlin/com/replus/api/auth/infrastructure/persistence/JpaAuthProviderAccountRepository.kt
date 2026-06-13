package com.replus.api.auth.infrastructure.persistence

import com.replus.api.auth.domain.model.AuthProvider
import com.replus.api.auth.domain.model.AuthProviderAccount
import com.replus.api.auth.domain.repository.AuthProviderAccountRepository
import org.springframework.stereotype.Repository

@Repository
class JpaAuthProviderAccountRepository(
    private val authProviderAccountJpaRepository: AuthProviderAccountJpaRepository,
) : AuthProviderAccountRepository {
    override fun findByProviderAndProviderSubject(
        provider: AuthProvider,
        providerSubject: String,
    ): AuthProviderAccount? =
        authProviderAccountJpaRepository.findByProviderAndProviderSubject(provider, providerSubject)?.toDomain()

    override fun save(account: AuthProviderAccount): AuthProviderAccount =
        authProviderAccountJpaRepository.save(AuthProviderAccountEntity.from(account)).toDomain()
}
