package com.replus.api.auth.infrastructure.persistence

import com.replus.api.auth.domain.model.User
import com.replus.api.auth.domain.repository.UserRepository
import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaUserRepository(
    private val userJpaRepository: UserJpaRepository,
) : UserRepository {
    override fun existsById(userId: UUID): Boolean =
        userJpaRepository.existsById(userId)

    override fun getById(userId: UUID): User =
        userJpaRepository.findByIdOrNull(userId)?.toDomain()
            ?: throw CoreException(ErrorType.UNAUTHENTICATED)

    override fun save(user: User): User =
        userJpaRepository.save(UserEntity.from(user)).toDomain()
}
