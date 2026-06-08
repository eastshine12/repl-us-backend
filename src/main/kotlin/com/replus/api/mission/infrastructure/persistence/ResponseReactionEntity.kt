package com.replus.api.mission.infrastructure.persistence

import com.replus.api.mission.domain.model.ReactionType
import com.replus.api.mission.domain.model.ResponseReaction
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "response_reactions")
class ResponseReactionEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "response_id", nullable = false)
    var responseId: UUID,

    @Column(name = "member_id", nullable = false)
    var memberId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    var type: ReactionType,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
) {
    fun toDomain(): ResponseReaction =
        ResponseReaction(
            id = id,
            responseId = responseId,
            memberId = memberId,
            type = type,
            createdAt = createdAt,
        )

    companion object {
        fun from(reaction: ResponseReaction): ResponseReactionEntity =
            ResponseReactionEntity(
                id = reaction.id,
                responseId = reaction.responseId,
                memberId = reaction.memberId,
                type = reaction.type,
                createdAt = reaction.createdAt,
            )
    }
}
