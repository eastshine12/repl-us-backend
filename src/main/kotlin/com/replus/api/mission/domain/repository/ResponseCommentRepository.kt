package com.replus.api.mission.domain.repository

import com.replus.api.mission.domain.model.ResponseComment

interface ResponseCommentRepository {
    fun save(comment: ResponseComment): ResponseComment
}
