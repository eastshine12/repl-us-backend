package com.replus.api.mission.infrastructure.persistence

import com.replus.api.mission.domain.model.ResponseReaction
import com.replus.api.mission.domain.repository.ResponseReactionRepository
import org.springframework.stereotype.Repository

@Repository
class JpaResponseReactionRepository(
    private val responseReactionJpaRepository: ResponseReactionJpaRepository,
) : ResponseReactionRepository {
    override fun save(reaction: ResponseReaction): ResponseReaction =
        responseReactionJpaRepository.save(ResponseReactionEntity.from(reaction)).toDomain()
}
