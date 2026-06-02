package com.replus.api.mission.infrastructure.persistence

import com.replus.api.mission.domain.model.ResponseReaction
import com.replus.api.mission.domain.repository.ResponseReactionRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaResponseReactionRepository(
    private val responseReactionJpaRepository: ResponseReactionJpaRepository,
) : ResponseReactionRepository {
    override fun save(reaction: ResponseReaction): ResponseReaction =
        responseReactionJpaRepository.save(ResponseReactionEntity.from(reaction)).toDomain()

    override fun findAllByResponseIds(responseIds: Collection<UUID>): List<ResponseReaction> =
        responseReactionJpaRepository.findAllByResponseIdIn(responseIds).map { it.toDomain() }
}
