package com.replus.api.mission.infrastructure.persistence

import com.replus.api.mission.domain.model.ResponseComment
import com.replus.api.mission.domain.repository.ResponseCommentRepository
import org.springframework.stereotype.Repository

@Repository
class JpaResponseCommentRepository(
    private val responseCommentJpaRepository: ResponseCommentJpaRepository,
) : ResponseCommentRepository {
    override fun save(comment: ResponseComment): ResponseComment =
        responseCommentJpaRepository.save(ResponseCommentEntity.from(comment)).toDomain()
}
