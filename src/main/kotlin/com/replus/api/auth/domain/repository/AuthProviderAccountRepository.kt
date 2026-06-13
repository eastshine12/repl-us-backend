package com.replus.api.auth.domain.repository

import com.replus.api.auth.domain.model.AuthProvider
import com.replus.api.auth.domain.model.AuthProviderAccount

interface AuthProviderAccountRepository {
    fun findByProviderAndProviderSubject(
        provider: AuthProvider,
        providerSubject: String,
    ): AuthProviderAccount?

    fun save(account: AuthProviderAccount): AuthProviderAccount
}
