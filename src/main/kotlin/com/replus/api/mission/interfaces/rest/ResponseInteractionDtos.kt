package com.replus.api.mission.interfaces.rest

import com.replus.api.common.interfaces.rest.dto.UserSummaryResponse
import com.replus.api.mission.application.CommentListResult
import com.replus.api.mission.application.CreatedCommentResult
import com.replus.api.mission.application.CreatedReactionResult
import com.replus.api.mission.domain.model.ReactionType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateReactionRequest(
    val type: ReactionType,
)

data class ReactionResponse(
    val id: UUID,
    val responseId: UUID,
    val memberId: UUID,
    val type: ReactionType,
    val createdAt: Instant,
)

data class CreateCommentRequest(
    @field:NotBlank
    @field:Size(max = 80)
    val body: String,
)

data class CommentResponse(
    val id: UUID,
    val responseId: UUID,
    val memberId: UUID,
    val author: UserSummaryResponse,
    val body: String,
    val createdAt: Instant,
    val deletedAt: Instant?,
)

data class CommentListResponse(
    val comments: List<CommentResponse>,
)

fun CreatedReactionResult.toResponse(): ReactionResponse =
    ReactionResponse(
        id = reaction.id,
        responseId = reaction.responseId,
        memberId = reaction.memberId,
        type = reaction.type,
        createdAt = reaction.createdAt,
    )

fun CreatedCommentResult.toResponse(): CommentResponse =
    CommentResponse(
        id = comment.id,
        responseId = comment.responseId,
        memberId = comment.memberId,
        author = UserSummaryResponse(
            id = author.id,
            displayName = author.displayName,
            avatarUrl = author.avatarUrl,
        ),
        body = comment.body,
        createdAt = comment.createdAt,
        deletedAt = comment.deletedAt,
    )

fun CommentListResult.toResponse(): CommentListResponse =
    CommentListResponse(
        comments = comments.map {
            CommentResponse(
                id = it.comment.id,
                responseId = it.comment.responseId,
                memberId = it.comment.memberId,
                author = UserSummaryResponse(
                    id = it.author.id,
                    displayName = it.author.displayName,
                    avatarUrl = it.author.avatarUrl,
                ),
                body = it.comment.body,
                createdAt = it.comment.createdAt,
                deletedAt = it.comment.deletedAt,
            )
        },
    )
