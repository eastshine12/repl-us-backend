package com.replus.api.mission.domain.repository

import com.replus.api.mission.domain.model.ResponseComment
import java.time.Instant
import java.util.UUID

interface ResponseCommentRepository {
    fun save(comment: ResponseComment): ResponseComment

    fun findActiveByResponseId(responseId: UUID): List<ResponseComment>

    fun softDeleteByResponseId(responseId: UUID, deletedAt: Instant)
}
