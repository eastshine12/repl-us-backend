package com.replus.api.mission.infrastructure.persistence

import com.replus.api.mission.domain.model.ResponseComment
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "response_comments")
class ResponseCommentEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "response_id", nullable = false)
    var responseId: UUID,

    @Column(name = "member_id", nullable = false)
    var memberId: UUID,

    @Column(name = "body", nullable = false, length = 80)
    var body: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "deleted_at")
    var deletedAt: Instant?,
) {
    fun toDomain(): ResponseComment =
        ResponseComment(
            id = id,
            responseId = responseId,
            memberId = memberId,
            body = body,
            createdAt = createdAt,
            deletedAt = deletedAt,
        )

    companion object {
        fun from(comment: ResponseComment): ResponseCommentEntity =
            ResponseCommentEntity(
                id = comment.id,
                responseId = comment.responseId,
                memberId = comment.memberId,
                body = comment.body,
                createdAt = comment.createdAt,
                deletedAt = comment.deletedAt,
            )
    }
}
