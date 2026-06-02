package com.replus.api.mission.domain.repository

import com.replus.api.mission.domain.model.ResponseReaction
import java.util.UUID

interface ResponseReactionRepository {
    fun save(reaction: ResponseReaction): ResponseReaction

    fun findAllByResponseIds(responseIds: Collection<UUID>): List<ResponseReaction>
}
