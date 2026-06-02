package com.replus.api.mission.infrastructure.persistence

import com.replus.api.mission.domain.model.ResponseComment
import com.replus.api.mission.domain.repository.ResponseCommentRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaResponseCommentRepository(
    private val responseCommentJpaRepository: ResponseCommentJpaRepository,
) : ResponseCommentRepository {
    override fun save(comment: ResponseComment): ResponseComment =
        responseCommentJpaRepository.save(ResponseCommentEntity.from(comment)).toDomain()

    override fun findActiveByResponseId(responseId: UUID): List<ResponseComment> =
        responseCommentJpaRepository
            .findAllByResponseIdAndDeletedAtIsNullOrderByCreatedAtAsc(responseId)
            .map { it.toDomain() }
}
