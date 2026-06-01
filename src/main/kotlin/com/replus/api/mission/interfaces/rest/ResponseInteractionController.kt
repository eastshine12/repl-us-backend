package com.replus.api.mission.interfaces.rest

import com.replus.api.common.security.BearerAuthSupport
import com.replus.api.mission.application.ResponseInteractionFacade
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class ResponseInteractionController(
    private val responseInteractionFacade: ResponseInteractionFacade,
    private val bearerAuthSupport: BearerAuthSupport,
) {
    @PostMapping("/api/rooms/{roomId}/responses/{responseId}/reactions")
    @ResponseStatus(HttpStatus.CREATED)
    fun createReaction(
        @RequestHeader(BearerAuthSupport.AUTHORIZATION_HEADER, required = false)
        authorization: String?,
        @PathVariable roomId: UUID,
        @PathVariable responseId: UUID,
        @Valid @RequestBody
        request: CreateReactionRequest,
    ): ReactionResponse {
        val user = bearerAuthSupport.requireUser(authorization)
        return responseInteractionFacade.createReaction(
            userId = user.userId,
            roomId = roomId,
            responseId = responseId,
            type = request.type,
        ).toResponse()
    }

    @PostMapping("/api/rooms/{roomId}/responses/{responseId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    fun createComment(
        @RequestHeader(BearerAuthSupport.AUTHORIZATION_HEADER, required = false)
        authorization: String?,
        @PathVariable roomId: UUID,
        @PathVariable responseId: UUID,
        @Valid @RequestBody
        request: CreateCommentRequest,
    ): CommentResponse {
        val user = bearerAuthSupport.requireUser(authorization)
        return responseInteractionFacade.createComment(
            userId = user.userId,
            roomId = roomId,
            responseId = responseId,
            body = request.body,
        ).toResponse()
    }
}
