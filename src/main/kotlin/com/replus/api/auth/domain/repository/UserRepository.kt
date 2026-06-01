package com.replus.api.auth.domain.repository

import com.replus.api.auth.domain.model.User
import java.util.UUID

interface UserRepository {
    fun existsById(userId: UUID): Boolean

    fun getById(userId: UUID): User

    fun save(user: User): User
}
