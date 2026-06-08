package com.replus.api.mission.application

import com.replus.api.auth.domain.model.User
import com.replus.api.mission.domain.model.ResponseComment
import com.replus.api.mission.domain.model.ResponseReaction

data class CreatedReactionResult(
    val reaction: ResponseReaction,
)

data class CreatedCommentResult(
    val comment: ResponseComment,
    val author: User,
)

data class CommentListResult(
    val comments: List<CommentWithAuthorResult>,
)

data class CommentWithAuthorResult(
    val comment: ResponseComment,
    val author: User,
)
