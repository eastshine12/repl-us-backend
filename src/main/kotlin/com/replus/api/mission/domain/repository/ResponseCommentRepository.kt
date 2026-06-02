package com.replus.api.mission.domain.repository

import com.replus.api.mission.domain.model.ResponseComment
import java.util.UUID

interface ResponseCommentRepository {
    fun save(comment: ResponseComment): ResponseComment

    fun findActiveByResponseId(responseId: UUID): List<ResponseComment>
}
