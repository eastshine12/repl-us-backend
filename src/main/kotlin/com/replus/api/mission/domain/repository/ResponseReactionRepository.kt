package com.replus.api.mission.domain.repository

import com.replus.api.mission.domain.model.ResponseReaction

interface ResponseReactionRepository {
    fun save(reaction: ResponseReaction): ResponseReaction
}
